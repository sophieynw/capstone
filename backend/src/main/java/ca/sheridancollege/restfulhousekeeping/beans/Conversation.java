package ca.sheridancollege.restfulhousekeeping.beans;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
	uniqueConstraints = @UniqueConstraint(columnNames = {"manager_id", "cleaner_id"})	
	)
public class Conversation {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(optional = false)
	@JoinColumn(name = "organization_id", nullable = false)
	private Organization organization;
	
	@ManyToOne(optional = false)
	@JoinColumn(name = "manager_id", nullable = false)
	private User manager;
	
	@ManyToOne(optional = false)
	@JoinColumn(name = "cleaner_id", nullable = false)
	private User cleaner;
	
	@Column(nullable = false)
	private LocalDateTime createdAt;
	
	@PrePersist
	public void setCreatedAt() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}

}
