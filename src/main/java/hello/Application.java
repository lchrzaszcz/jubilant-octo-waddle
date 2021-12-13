package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@SpringBootApplication
@RestController
public class Application {

    private int previousScore = -100;
    private boolean randomMovedPreviously = false;

  static class Self {
    public String href;
  }

  static class Links {
    public Self self;
  }

  static class PlayerState {
    public Integer x;
    public Integer y;
    public String direction;
    public Boolean wasHit;
    public Integer score;
  }

  static class Arena {
    public List<Integer> dims;
    public Map<String, PlayerState> state;
  }

  static class ArenaUpdate {
    public Links _links;
    public Arena arena;
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.initDirectFieldAccess();
  }

  @GetMapping("/")
  public String index() {
    return "Let the battle begin!";
  }

  private long countTargetingEnemies(ArenaUpdate arenaUpdate) {
    String self = arenaUpdate._links.self.href;
    PlayerState myState = arenaUpdate.arena.state.get(self);

    long enemyCount = arenaUpdate.arena.state.values().stream()
        .filter(player -> 
            (player.x == myState.x && player.y < myState.y && player.y >= myState.y - 3 && player.direction.equals("S")) ||
            (player.x == myState.x && player.y > myState.y && player.y <= myState.y + 3 && player.direction.equals("N")) ||
            (player.y == myState.y && player.x > myState.x && player.x <= myState.x + 3 && player.direction.equals("W")) ||
            (player.y == myState.y && player.x < myState.x && player.x >= myState.x - 3 && player.direction.equals("E"))
        ).count();

        return enemyCount;
    }

  private boolean haveTarget(ArenaUpdate arenaUpdate) {
      String self = arenaUpdate._links.self.href;
      PlayerState myState = arenaUpdate.arena.state.get(self);

      if (myState.direction.equals("N")) {
        return arenaUpdate.arena.state.values().stream().anyMatch(player -> player.x == myState.x && player.y < myState.y && player.y >= myState.y - 3);
      } else if (myState.direction.equals("S")) {
        return arenaUpdate.arena.state.values().stream().anyMatch(player -> player.x == myState.x && player.y > myState.y && player.y <= myState.y + 3);
      } else if (myState.direction.equals("E")) {
        return arenaUpdate.arena.state.values().stream().anyMatch(player -> player.y == myState.y && player.x > myState.x && player.x <= myState.x + 3);
      } else {
        return arenaUpdate.arena.state.values().stream().anyMatch(player -> player.y == myState.y && player.x < myState.x && player.x >= myState.x - 3);
      }
  }

  private String randomMove() {
    String[] commands = new String[]{"F", "R", "L"};
    int i = new Random().nextInt(3);
    return commands[i];
  }

  @PostMapping("/**")
  public String index(@RequestBody ArenaUpdate arenaUpdate) {
    System.out.println(arenaUpdate);
    long enemyTargetingCount = countTargetingEnemies(arenaUpdate);
    String command = null;

    String self = arenaUpdate._links.self.href;
    PlayerState myState = arenaUpdate.arena.state.get(self);

    if (myState.score < previousScore && !randomMovedPreviously) {
        randomMovedPreviously = true;
        command = randomMove();
    }

    if (enemyTargetingCount > 1) {
        randomMovedPreviously = false;
        command = "F";
    }

    if (haveTarget(arenaUpdate)) {
        randomMovedPreviously = false;
        command = "T";
    }

    if (enemyTargetingCount == 1) {
        randomMovedPreviously = false;
        command = "F";
    }

    if (command == null) {
        command = randomMove();

    }
    
    previousScore = myState.score;

    return command;
  }

}

