package hr.algebra.quickpoll.service;

import hr.algebra.quickpoll.model.Poll;
import hr.algebra.quickpoll.model.PollOption;
import hr.algebra.quickpoll.repository.PollOptionRepository;
import hr.algebra.quickpoll.repository.PollRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PollService {

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;

    public PollService(PollRepository pollRepository, PollOptionRepository pollOptionRepository) {
        this.pollRepository = pollRepository;
        this.pollOptionRepository = pollOptionRepository;
    }

    public List<Poll> findAll() {
        return pollRepository.findAllByOrderByCreatedAtDesc();
    }

    public Poll findById(Long id) {
        return pollRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found: " + id));
    }

    @Transactional
    public Poll create(String question, List<String> optionTexts) {
        Poll poll = new Poll(question);
        for (String text : optionTexts) {
            if (text != null && !text.isBlank()) {
                poll.addOption(new PollOption(text.trim()));
            }
        }
        return pollRepository.save(poll);
    }

    @Transactional
    public void vote(Long pollId, Long optionId) {
        Poll poll = findById(pollId);
        PollOption option = poll.getOptions().stream()
                .filter(o -> o.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Option " + optionId + " does not belong to poll " + pollId));
        option.incrementVotes();
        pollOptionRepository.save(option);
    }
}
