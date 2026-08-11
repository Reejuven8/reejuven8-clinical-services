package com.reejuven8.ninemo.community.repository;

import com.reejuven8.ninemo.community.model.ContentArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentArticleRepository extends MongoRepository<ContentArticle, String> {
    Page<ContentArticle> findByIsPublishedTrueOrderByPublishedAtDesc(Pageable pageable);
    List<ContentArticle> findByIsPublishedTrueAndCategoryOrderByPublishedAtDesc(String category);
    List<ContentArticle> findByIsPublishedTrueAndGestationalWeeksContaining(int gestationalWeek);
}
