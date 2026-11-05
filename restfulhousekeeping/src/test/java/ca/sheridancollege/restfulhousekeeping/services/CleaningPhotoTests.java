package ca.sheridancollege.restfulhousekeeping.services;

import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.beans.Property;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningPhotoRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CleaningPhotoTests {

    private static final String PHOTOS_PATH = "/photos/cleaning/{cleaningId}";
    private static final String CLOUD_URL = "https://ik.imagekit.io/test/restful-housekeeping/cleaning-pictures/photo.png";
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aT1sAAAAASUVORK5CYII=");

    @Autowired MockMvc mvc;
    @Autowired CleaningRepository cleanings;
    @Autowired PropertyRepository properties;
    @Autowired UserRepository users;
    @Autowired CleaningPhotoRepository photos;
    @MockitoBean ImageKitService imageKit;

    private User manager;
    private User cleaner;
    private User unrelatedCleaner;
    private Cleaning cleaning;

    @BeforeEach
    void setup() {
        manager = saveUser("photo-manager", Role.MANAGER);
        cleaner = saveUser("photo-cleaner", Role.CLEANER);
        unrelatedCleaner = saveUser("other-photo-cleaner", Role.CLEANER);
        Property property = properties.save(Property.builder().name("Photo test").manager(manager).build());
        cleaning = cleanings.save(Cleaning.builder().manager(manager).cleaner(cleaner).property(property).build());
    }

    @Test
    void assignedCleanerUploadsAndBothParticipantsCanReadThePhoto() throws Exception {
        when(imageKit.uploadCleaningPhoto(eq(PNG), endsWith(".png")))
                .thenReturn(new ImageKitService.UploadedImage(CLOUD_URL, "cloud-id"));
        when(imageKit.readCleaningPhoto(CLOUD_URL)).thenReturn(PNG);

        mvc.perform(multipart(PHOTOS_PATH, cleaning.getId()).file(photo(PNG)).with(as(cleaner)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        String list = mvc.perform(get(PHOTOS_PATH, cleaning.getId()).with(as(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        Number photoId = com.jayway.jsonpath.JsonPath.read(list, "$[0].id");
        assertThat(photos.findById(photoId.longValue())).hasValueSatisfying(photo -> {
            assertThat(photo.getImageKitFileId()).isEqualTo("cloud-id");
            assertThat(photo.getImageKitUrl()).isEqualTo(CLOUD_URL);
            assertThat(photo.getImageData()).isNull();
        });

        for (User participant : new User[]{manager, cleaner}) {
            mvc.perform(get(PHOTOS_PATH + "/{photoId}", cleaning.getId(), photoId).with(as(participant)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_PNG))
                    .andExpect(content().bytes(PNG));
        }

        mvc.perform(get(PHOTOS_PATH, cleaning.getId()).with(as(unrelatedCleaner)))
                .andExpect(status().isForbidden());
        mvc.perform(get(PHOTOS_PATH + "/{photoId}", cleaning.getId(), photoId).with(as(unrelatedCleaner)))
                .andExpect(status().isForbidden());

        TestTransaction.flagForRollback();
        TestTransaction.end();
        verify(imageKit).delete("cloud-id");
    }

    @Test
    void blocksOtherUploadersAndNonImages() throws Exception {
        for (User user : new User[]{manager, unrelatedCleaner}) {
            mvc.perform(multipart(PHOTOS_PATH, cleaning.getId()).file(photo(PNG)).with(as(user)))
                    .andExpect(status().isForbidden());
        }

        mvc.perform(multipart(PHOTOS_PATH, cleaning.getId()).file(photo(new byte[]{1, 2, 3})).with(as(cleaner)))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(imageKit);
    }

    private User saveUser(String username, Role role) {
        return users.save(User.builder().username(username).email(username + "@example.com").role(role).build());
    }

    private static MockMultipartFile photo(byte[] bytes) {
        return new MockMultipartFile("file", "photo.png", "image/png", bytes);
    }

    private static RequestPostProcessor as(User user) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
    }
}
