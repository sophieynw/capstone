package ca.sheridancollege.restfulhousekeeping.repositories;

import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
	
	public Optional<User> findByUsernameIgnoreCase(String username);

	public Optional<User> findByEmailIgnoreCase(String email);
	
	public List<User> findAllByOrganization_IdAndRole(Long organizationId, Role role);

	public Optional<User> findFirstByOrganization_IdAndRoleOrderByIdAsc(Long organizationId, Role role);

	public boolean existsByUsernameIgnoreCase(String username);

	public boolean existsByEmailIgnoreCase(String email);
	
}
