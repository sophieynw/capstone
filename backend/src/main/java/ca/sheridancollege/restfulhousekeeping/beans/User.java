package ca.sheridancollege.restfulhousekeeping.beans;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "_user")
public class User implements UserDetails {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@ManyToOne
	@JoinColumn(name="organizationId")
	private Organization organization;
	private String firstName;
	private String lastName;
	private String username;
	@Column(nullable = false, unique = true)
	private String email;
	private String password;
	private String phoneNumber;
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "profile_photo_id", unique = true)
	@JsonIgnore
	@EqualsAndHashCode.Exclude
	@ToString.Exclude
	private Photo profilePhoto;
	
	@Enumerated(EnumType.STRING)
	@NonNull
	@Column(nullable = false)
	private Role role;
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(role.name()));
	}
	@Override
	public @Nullable String getPassword() {
		return password;
	}
	@Override
	public String getUsername() {
		return username;
	}
	

	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	public String getProfilePicturePath() {
		return profilePhoto == null ? null : profilePhoto.getImageKitUrl();
	}
}
