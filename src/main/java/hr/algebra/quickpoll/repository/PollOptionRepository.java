package hr.algebra.quickpoll.repository;

import hr.algebra.quickpoll.model.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {
}
