package com.bookStoreFullStack.entity;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "book")
public class Book {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	
	@Column(name = "name")
	private String name;
	
	@Column(name = "averageStars")
	private double averageStars;
	
	@ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
	
	@Column(name = "price")
	private double price;
	
	@Column(name = "image")
	private String image;
	
	@Column(name = "status")
	private String status;
	
	@Column(name = "description", columnDefinition = "TEXT")
	private String description;
	
	@Column(name = "quantity")
	private int quantity;
	
	@ManyToOne
    @JoinColumn(name = "author_id")
    private Author author;
	
	@OneToMany(mappedBy = "book")
    private List<LikeRating> likes;

	@OneToMany(mappedBy = "book")
    private List<Rating> ratings;
	
}