package com.portfolio.experience.entity;

import com.portfolio.common.content.ContentStatus;
import com.portfolio.media.entity.Media;
import com.portfolio.technology.entity.Technology;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * One role held. Column shape mirrors V5__create_experience_tables.sql exactly.
 *
 * <p>Shares the {@code technology} lookup with Project through {@code experience_technology}, so
 * "which technologies has this person actually worked with?" spans both.
 */
@Entity
@Table(name = "experience")
@SQLDelete(sql = "UPDATE experience SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Experience {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "company", nullable = false, length = 200)
	private String company;

	/** The employer's own site. Nullable: not every employer has one worth linking. */
	@Column(name = "company_url", length = 500)
	private String companyUrl;

	@Column(name = "position", nullable = false, length = 200)
	private String position;

	@Enumerated(EnumType.STRING)
	@Column(name = "employment_type", length = 20)
	private EmploymentType employmentType;

	@Column(name = "description", columnDefinition = "text")
	private String description;

	@Column(name = "responsibilities", columnDefinition = "text")
	private String responsibilities;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	@Column(name = "currently_working", nullable = false)
	private boolean currentlyWorking;

	/** Null rather than an exception when the logo has since been deleted (D-019). */
	@ManyToOne(fetch = FetchType.LAZY)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "company_logo_media_id")
	private Media companyLogo;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ContentStatus status = ContentStatus.DRAFT;

	@Column(name = "ai_visible", nullable = false)
	private boolean aiVisible;

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "experience_technology",
			joinColumns = @JoinColumn(name = "experience_id"),
			inverseJoinColumns = @JoinColumn(name = "technology_id"))
	private Set<Technology> technologies = new LinkedHashSet<>();

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public Experience() {
		// JPA / new row
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public void replaceTechnologies(Set<Technology> replacements) {
		technologies.clear();
		technologies.addAll(replacements);
	}

	public Long getId() {
		return id;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getCompanyUrl() {
		return companyUrl;
	}

	public void setCompanyUrl(String companyUrl) {
		this.companyUrl = companyUrl;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public EmploymentType getEmploymentType() {
		return employmentType;
	}

	public void setEmploymentType(EmploymentType employmentType) {
		this.employmentType = employmentType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getResponsibilities() {
		return responsibilities;
	}

	public void setResponsibilities(String responsibilities) {
		this.responsibilities = responsibilities;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public boolean isCurrentlyWorking() {
		return currentlyWorking;
	}

	public void setCurrentlyWorking(boolean currentlyWorking) {
		this.currentlyWorking = currentlyWorking;
	}

	public Media getCompanyLogo() {
		return companyLogo;
	}

	public void setCompanyLogo(Media companyLogo) {
		this.companyLogo = companyLogo;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
	}

	public ContentStatus getStatus() {
		return status;
	}

	public void setStatus(ContentStatus status) {
		this.status = status;
	}

	public boolean isAiVisible() {
		return aiVisible;
	}

	public void setAiVisible(boolean aiVisible) {
		this.aiVisible = aiVisible;
	}

	public Set<Technology> getTechnologies() {
		return technologies;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
