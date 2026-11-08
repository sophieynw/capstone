package ca.sheridancollege.restfulhousekeeping.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.lang.reflect.Proxy;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ca.sheridancollege.restfulhousekeeping.beans.*;
import ca.sheridancollege.restfulhousekeeping.config.JwtAuthenticationFilter;
import ca.sheridancollege.restfulhousekeeping.config.SecurityConfig;
import ca.sheridancollege.restfulhousekeeping.repositories.SlotRepository;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {AvailabilitySlotControllerTests.TestConfig.class, SecurityConfig.class})
class AvailabilitySlotControllerTests {
    @Autowired WebApplicationContext context;
    @Autowired SlotStore store;
    MockMvc mvc;
    User cleaner = User.builder().id(1L).username("cleaner").role(Role.CLEANER).build();
    User manager = User.builder().id(2L).username("manager").role(Role.MANAGER).build();

    @BeforeEach
    void setup() {
        store.slots.clear();
        store.nextId = 10L;
        store.slots.put(7L, AvailabilitySlot.builder().id(7L).cleaner(cleaner)
                .dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0)).build());
        User other = User.builder().id(3L).username("other").role(Role.CLEANER).build();
        store.slots.put(8L, AvailabilitySlot.builder().id(8L).cleaner(other)
                .dayOfWeek(DayOfWeek.TUESDAY).startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0)).build());
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void managersAndAnonymousUsersCannotUseAnySlotEndpoint() throws Exception {
        for (User user : new User[] {manager, null}) {
            for (MockHttpServletRequestBuilder request : new MockHttpServletRequestBuilder[] {
                    get("/availability-slots"), get("/availability-slots/7"),
                    get("/availability-slots/cleaner/1"), post("/availability-slots").content(validSlot()),
                    patch("/availability-slots/7").content(validSlot()), delete("/availability-slots/7")}) {
                if (user != null) request.with(authentication(auth(user)));
                mvc.perform(request.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isForbidden());
            }
        }
        assertThat(store.slots).hasSize(2);
        assertThat(store.slots.get(7L).getStartTime()).isEqualTo(LocalTime.of(9, 0));
    }

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void createsAvailabilityForEveryDayUsingAuthenticatedOwner(DayOfWeek day) throws Exception {
        mvc.perform(asCleaner(post("/availability-slots").content("""
                {"dayOfWeek":"%s","startTime":"09:00","endTime":"17:00","cleaner":{"id":3}}
                """.formatted(day))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.dayOfWeek").value(day.name()))
                .andExpect(jsonPath("$.cleaner.id").value(1));
        assertThat(store.slots.get(10L).getCleaner().getId()).isEqualTo(1L);
    }

    @Test
    void readsOnlyOwnSlots() throws Exception {
        mvc.perform(asCleaner(get("/availability-slots")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(7));
        mvc.perform(asCleaner(get("/availability-slots/cleaner/1")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(asCleaner(get("/availability-slots/7"))).andExpect(status().isOk());
        mvc.perform(asCleaner(get("/availability-slots/8"))).andExpect(status().isNotFound());
        mvc.perform(asCleaner(get("/availability-slots/cleaner/3"))).andExpect(status().isForbidden());
    }

    @Test
    void cannotUpdateOrDeleteAnotherCleanersSlots() throws Exception {
        mvc.perform(asCleaner(patch("/availability-slots/8").content(validSlot())))
                .andExpect(status().isNotFound());
        mvc.perform(asCleaner(delete("/availability-slots/8"))).andExpect(status().isNotFound());
        assertThat(store.slots.get(8L).getStartTime()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    void updatesAndDeletesOwnSlotAndHandlesMissingSlots() throws Exception {
        mvc.perform(asCleaner(patch("/availability-slots/7")
                .content("{\"startTime\":\"10:00\",\"endTime\":\"18:00\"}")))
                .andExpect(status().isOk());
        assertThat(store.slots.get(7L).getStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(store.slots.get(7L).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        mvc.perform(asCleaner(delete("/availability-slots/7"))).andExpect(status().isNoContent());
        assertThat(store.slots).doesNotContainKey(7L);
        mvc.perform(asCleaner(delete("/availability-slots/7"))).andExpect(status().isNotFound());
        mvc.perform(asCleaner(patch("/availability-slots/7").content(validSlot())))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "{\"startTime\":null,\"endTime\":\"17:00\"}",
        "{\"startTime\":\"09:00\",\"endTime\":null}",
        "{\"startTime\":\"17:00\",\"endTime\":\"09:00\"}",
        "{\"startTime\":\"09:00\",\"endTime\":\"09:00\"}",
        "{\"startTime\":\"25:00\",\"endTime\":\"17:00\"}"
    })
    void rejectsInvalidTimesOnCreateAndUpdate(String times) throws Exception {
        String create = "{\"dayOfWeek\":\"MONDAY\"," + times.substring(1);
        if (times.equals("{}")) create = "{\"dayOfWeek\":\"MONDAY\"}";
        mvc.perform(asCleaner(post("/availability-slots").content(create)))
                .andExpect(status().isBadRequest());
        mvc.perform(asCleaner(patch("/availability-slots/7").content(times)))
                .andExpect(status().isBadRequest());
        assertThat(store.slots).hasSize(2);
        assertThat(store.slots.get(7L).getStartTime()).isEqualTo(LocalTime.of(9, 0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"FUNDAY\""})
    void rejectsMissingOrInvalidDay(String day) throws Exception {
        mvc.perform(asCleaner(post("/availability-slots").content(
                "{\"dayOfWeek\":" + day + ",\"startTime\":\"09:00\",\"endTime\":\"17:00\"}")))
                .andExpect(status().isBadRequest());
        assertThat(store.slots).hasSize(2);
    }

    private MockHttpServletRequestBuilder asCleaner(MockHttpServletRequestBuilder request) {
        return request.with(authentication(auth(cleaner))).contentType(MediaType.APPLICATION_JSON);
    }

    private Authentication auth(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    private String validSlot() {
        return "{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"09:00\",\"endTime\":\"17:00\"}";
    }

    static class SlotStore {
        final Map<Long, AvailabilitySlot> slots = new HashMap<>();
        long nextId = 10L;

        SlotRepository repository() {
            return (SlotRepository) Proxy.newProxyInstance(SlotRepository.class.getClassLoader(),
                    new Class<?>[] {SlotRepository.class}, (proxy, method, args) -> {
                return switch (method.getName()) {
                    case "findByCleanerId" -> slots.values().stream()
                            .filter(slot -> slot.getCleaner().getId().equals(args[0])).toList();
                    case "findByIdAndCleanerId" -> Optional.ofNullable(slots.get(args[0]))
                            .filter(slot -> slot.getCleaner().getId().equals(args[1]));
                    case "save" -> {
                        AvailabilitySlot slot = (AvailabilitySlot) args[0];
                        if (slot.getId() == null) slot.setId(nextId++);
                        slots.put(slot.getId(), slot);
                        yield slot;
                    }
                    case "delete" -> { slots.remove(((AvailabilitySlot) args[0]).getId()); yield null; }
                    case "toString" -> "SlotStore";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            });
        }
    }

    @Configuration
    @EnableWebMvc
    static class TestConfig {
        @Bean SlotStore slotStore() { return new SlotStore(); }
        @Bean SlotRepository slotRepository(SlotStore store) { return store.repository(); }
        @Bean AvailabilitySlotController controller(SlotRepository repository) {
            return new AvailabilitySlotController(repository);
        }
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter() {
            // Tests supply authentication through Spring Security's request support, without a bearer header.
            return new JwtAuthenticationFilter(null, null);
        }
        @Bean AuthenticationProvider authenticationProvider() {
            return new AuthenticationProvider() {
                public Authentication authenticate(Authentication auth) { return auth; }
                public boolean supports(Class<?> type) { return false; }
            };
        }
    }
}
