package hr.algebra.quickpoll.controller;

import hr.algebra.quickpoll.model.Poll;
import hr.algebra.quickpoll.service.PollService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PollRestController {

    private final PollService pollService;

    public PollRestController(PollService pollService) {
        this.pollService = pollService;
    }

    @GetMapping("/polls")
    public List<Poll> all() {
        return pollService.findAll();
    }

    @GetMapping("/polls/{id}")
    public Poll one(@PathVariable Long id) {
        return pollService.findById(id);
    }

    @PostMapping("/polls/{id}/vote")
    public Map<String, Object> vote(@PathVariable Long id, @RequestParam Long optionId) {
        pollService.vote(id, optionId);
        Poll updated = pollService.findById(id);
        return Map.of(
                "status", "ok",
                "pollId", id,
                "totalVotes", updated.totalVotes()
        );
    }
}
