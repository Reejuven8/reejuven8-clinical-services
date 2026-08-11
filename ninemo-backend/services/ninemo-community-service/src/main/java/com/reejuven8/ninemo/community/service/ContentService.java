package com.reejuven8.ninemo.community.service;

import com.reejuven8.ninemo.community.model.ContentArticle;
import com.reejuven8.ninemo.community.repository.ContentArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentArticleRepository contentArticleRepository;

    public Page<ContentArticle> listPublished(int page, int size) {
        return contentArticleRepository.findByIsPublishedTrueOrderByPublishedAtDesc(PageRequest.of(page, size));
    }

    public List<ContentArticle> listByCategory(String category) {
        return contentArticleRepository.findByIsPublishedTrueAndCategoryOrderByPublishedAtDesc(category);
    }

    public List<ContentArticle> listByGestationalWeek(int gestationalWeek) {
        return contentArticleRepository.findByIsPublishedTrueAndGestationalWeeksContaining(gestationalWeek);
    }
}
