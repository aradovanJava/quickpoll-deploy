package hr.algebra.quickpoll.controller;

import hr.algebra.quickpoll.service.PollService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class PollWebController {

    private final PollService pollService;

    @Value("${quickpoll.deployment.label:local}")
    private String deploymentLabel;

    public PollWebController(PollService pollService) {
        this.pollService = pollService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("polls", pollService.findAll());
        model.addAttribute("deploymentLabel", deploymentLabel);
        return "index";
    }

    @GetMapping("/polls/new")
    public String newPollForm(Model model) {
        model.addAttribute("deploymentLabel", deploymentLabel);
        return "new-poll";
    }

    @PostMapping("/polls")
    public String createPoll(@RequestParam String question,
                             @RequestParam List<String> options) {
        var poll = pollService.create(question, options);
        return "redirect:/polls/" + poll.getId();
    }

    @GetMapping("/polls/{id}")
    public String pollDetail(@PathVariable Long id, Model model) {
        model.addAttribute("poll", pollService.findById(id));
        model.addAttribute("deploymentLabel", deploymentLabel);
        return "poll-detail";
    }

    @PostMapping("/polls/{id}/vote")
    public String vote(@PathVariable Long id, @RequestParam Long optionId) {
        pollService.vote(id, optionId);
        return "redirect:/polls/" + id;
    }
}
