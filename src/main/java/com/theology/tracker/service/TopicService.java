package com.theology.tracker.service;

import com.theology.tracker.dto.TopicFormDto;
import com.theology.tracker.exception.ResourceNotFoundException;
import com.theology.tracker.model.Topic;
import com.theology.tracker.model.TopicType;
import com.theology.tracker.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class TopicService {

    private final TopicRepository topicRepo;

    public TopicService(TopicRepository topicRepo) {
        this.topicRepo = topicRepo;
    }

    @Transactional(readOnly = true)
    public List<Topic> findRoots() {
        return topicRepo.findByParentTopicIsNullOrderByTitleAsc();
    }

    @Transactional(readOnly = true)
    public List<Topic> findAllOrdered() {
        List<Topic> result = new ArrayList<>();
        for (Topic root : topicRepo.findByParentTopicIsNullOrderByTitleAsc()) {
            result.add(root);
            result.addAll(topicRepo.findByParentTopicIdOrderByTitleAsc(root.getId()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Topic findById(Long id) {
        return topicRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + id));
    }

    public Topic create(TopicFormDto form) {
        Topic topic = new Topic();
        applyForm(topic, form);
        return topicRepo.save(topic);
    }

    public Topic update(Long id, TopicFormDto form) {
        Topic topic = findById(id);
        applyForm(topic, form);
        return topicRepo.save(topic);
    }

    public void delete(Long id) {
        Topic topic = findById(id);
        for (Topic sub : topic.getSubtopics()) {
            sub.setParentTopic(null);
            topicRepo.save(sub);
        }
        topicRepo.delete(topic);
    }

    private void applyForm(Topic topic, TopicFormDto form) {
        if (form.title() == null || form.title().isBlank()) {
            throw new IllegalArgumentException("Topic title is required.");
        }
        topic.setTitle(form.title().trim());
        topic.setDescription(form.description() != null && !form.description().isBlank()
            ? form.description().trim() : null);
        if (form.type() != null && !form.type().isBlank()) {
            topic.setType(TopicType.valueOf(form.type().toUpperCase()));
        }
        if (form.parentTopicId() != null) {
            Topic parent = topicRepo.findById(form.parentTopicId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent topic not found"));
            if (parent.getId().equals(topic.getId())) {
                throw new IllegalArgumentException("A topic cannot be its own parent.");
            }
            topic.setParentTopic(parent);
        } else {
            topic.setParentTopic(null);
        }
    }
}
