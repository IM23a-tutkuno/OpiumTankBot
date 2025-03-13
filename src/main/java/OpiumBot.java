import dev.zwazel.GameWorld;
import dev.zwazel.PropertyHandler;
import dev.zwazel.bot.BotInterface;
import dev.zwazel.internal.PublicGameWorld;
import dev.zwazel.internal.connection.client.ConnectedClientConfig;
import dev.zwazel.internal.game.lobby.TeamConfig;
import dev.zwazel.internal.game.map.MapDefinition;
import dev.zwazel.internal.game.map.MarkerDefinition;
import dev.zwazel.internal.game.map.TileDefinition;
import dev.zwazel.internal.game.map.marker.FlagBase;
import dev.zwazel.internal.debug.MapVisualiser;
import dev.zwazel.internal.game.state.ClientState;
import dev.zwazel.internal.game.utils.Node;
import dev.zwazel.internal.game.tank.Tank;
import dev.zwazel.internal.game.tank.TankConfig;
import dev.zwazel.internal.game.tank.implemented.LightTank;
import dev.zwazel.internal.game.transform.Vec3;
import dev.zwazel.internal.message.MessageContainer;
import dev.zwazel.internal.message.MessageData;
import dev.zwazel.internal.message.data.GameConfig;
import dev.zwazel.internal.message.data.SimpleTextMessage;
import dev.zwazel.internal.message.data.tank.GotHit;
import dev.zwazel.internal.message.data.tank.Hit;


import java.util.LinkedList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static dev.zwazel.internal.message.MessageTarget.Type.CLIENT;

public class OpiumBot implements BotInterface {
    private final PropertyHandler propertyHandler = PropertyHandler.getInstance();
    private final float minAttackDistance;
    private final float maxAttackDistance;
    public int tick_count = 0;
    public List<int[]> path;
    public int array_pos = 0;
    public int load_counter = 0;


    private List<ConnectedClientConfig> teamMembers;
    private List<ConnectedClientConfig> enemyTeamMembers;

    private MapVisualiser visualiser;

    public OpiumBot() {
        this.minAttackDistance = Float.parseFloat(propertyHandler.getProperty("bot.attack.minDistance"));
        this.maxAttackDistance = Float.parseFloat(propertyHandler.getProperty("bot.attack.maxDistance"));
    }

    public void start() {
        // GameWorld.startGame(this, LightTank.class);
        // This starts the game with a LightTank, and immediately starts the game when connected
        GameWorld.connectToServer(this, LightTank.class); // This connects to the server with a LightTank, but does not immediately start the game


    }

    @Override
    public void setup(PublicGameWorld world) {
        GameConfig config = world.getGameConfig();

        TeamConfig myTeamConfig = config.getMyTeamConfig();
        TeamConfig enemyTeamConfig = config.teamConfigs().values().stream()
                .filter(teamConfig -> !teamConfig.teamName().equals(myTeamConfig.teamName()))
                .findFirst()
                .orElseThrow();

        // Get all team members, excluding myself
        teamMembers = config.getTeamMembers(myTeamConfig.teamName(), config.clientId());
        // Get all enemy team members
        enemyTeamMembers = config.getTeamMembers(enemyTeamConfig.teamName());

    }


    public double getZ(int x_tile, int y_tile, MapDefinition mapDefinition) {
        float[][] tiles = mapDefinition.tiles();
        return tiles[y_tile][x_tile];
    }

    @Override
    public void processTick(PublicGameWorld world) {


        ClientState myClientState = world.getMyState();
        ConnectedClientConfig myconfig = world.getGameConfig().getMyConfig();

        if (myClientState.state() == ClientState.PlayerState.DEAD) {
            System.out.println("I'm dead!");
            return;
        }

        LightTank tank = (LightTank) world.getTank();
        // HeavyTank tank = (HeavyTank) world.getTank();
        // SelfPropelledArtillery tank = (SelfPropelledArtillery) world.getTank();
        TankConfig myTankConfig = tank.getConfig(world);
        GameConfig config = world.getGameConfig();


        MapDefinition mapDefinition = config.mapDefinition();


        MarkerDefinition[] markers = config.mapDefinition().markers();
        String myTeam = myconfig.clientTeam();
        System.out.println("myTeam = " + myTeam);

        Vec3 my_coordinates = config.mapDefinition().getClosestTileFromWorld(myClientState.transformBody().getTranslation());

        int x_tank_tile = (int) my_coordinates.getX();
        int y_tank_tile = (int) my_coordinates.getZ();

        System.out.println("y_coord = " + y_tank_tile);
        System.out.println("x_coord = " + x_tank_tile);

        int[] my_path_coordinates = new int[]{x_tank_tile, y_tank_tile};
        int[] enemy_flag_coordinates = new int[0];
        float[][] map = mapDefinition.tiles();
        double tile_height = my_coordinates.getY();

        for (MarkerDefinition marker : markers) {
            if (marker.kind() instanceof FlagBase && !Objects.equals(marker.group(), myTeam)) {
                System.out.println("FlagBase found");
                System.out.println(marker.tile());
                long x = marker.tile().x();
                long y = marker.tile().y();
                int tile_x = (int) Math.floor(x);
                int tile_y = (int) Math.floor(y);


                Vec3 Flag_coordinate = Vec3.builder().x(tile_x).y(tile_y).z(tile_height).build();
                enemy_flag_coordinates = new int[]{tile_y, tile_x};
                System.out.println("enemy_flag_coordinates = " + Arrays.toString(enemy_flag_coordinates));
            }
        }


        int move_to_x = 0;
        int move_to_y = 0;

        if (this.tick_count == 10) {
            this.tick_count = 0;
        } else if (this.tick_count == 0) {
            this.path = AStarVisualizer.findPath(map, my_path_coordinates, enemy_flag_coordinates);
            System.out.println("loaded map!");
            System.out.println(Arrays.deepToString(this.path.toArray()));
            load_counter++;
            this.tick_count++;
        }



        System.out.println("my_path_coordinates = " + Arrays.toString(my_path_coordinates));
        System.out.println("this.path.get(0)[0] = " + this.path.get(array_pos)[0]);
        System.out.println("this.path.get(0)[0] = " + this.path.get(array_pos)[1]);

        if (this.path.get(array_pos)[0] == my_path_coordinates[1] && this.path.get(array_pos)[1] == my_path_coordinates[0]) {
            System.out.println("Condition true");
            array_pos++;
        }
        int pathY = this.path.get(array_pos)[0];
        int pathX = this.path.get(array_pos)[1];


        move_to_x = pathX;
        move_to_y = pathY;
        System.out.println("move_to_x = " + move_to_x);
        System.out.println("move_to_y = " + move_to_y);
        Vec3 move_coordinates = config.mapDefinition().getWorldTileCenter(move_to_x, move_to_y);

        tank.moveTowards(world, move_coordinates, false);





        // Vec3 move_coordinates = Vec3.builder().x(move_to_x).y(move_to_y).build();


        System.out.println("map = " + markers[0].kind());


        // Get the closest enemy tank
        Optional<ClientState> closestEnemy = enemyTeamMembers.stream()
                .map(connectedClientConfig -> world.getClientState(connectedClientConfig.clientId()))
                // Filter out null states, states without a position and dead states
                .filter(clientState -> clientState != null && clientState.transformBody().getTranslation() != null &&
                        clientState.state() != ClientState.PlayerState.DEAD)
                .min((o1, o2) -> {
                    double distance1 = myClientState.transformBody().getTranslation().distance(o1.transformBody().getTranslation());
                    double distance2 = myClientState.transformBody().getTranslation().distance(o2.transformBody().getTranslation());
                    return Double.compare(distance1, distance2);
                });

        // Move towards the closest enemy and shoot when close enough, or move in a circle if no enemies are found
        /* closestEnemy.ifPresentOrElse(
                enemy -> {
                    // If enemy is within attack range, shoot; otherwise, move accordingly
                    double distanceToEnemy = myClientState.transformBody().getTranslation().distance(enemy.transformBody().getTranslation());

                    if (distanceToEnemy < this.minAttackDistance) {
                        System.out.println("meow");
                        System.out.println(enemy.transformBody().getTranslation());
                        // Move away from enemy if too close
                        tank.moveTowards(world, Tank.MoveDirection.BACKWARD, enemy.transformBody().getTranslation(), true);
                    } else if (distanceToEnemy > this.maxAttackDistance) {
                        // Move towards enemy if too far
                        tank.moveTowards(world, Tank.MoveDirection.FORWARD, enemy.transformBody().getTranslation(), true);
                    }
                    tank.rotateTurretTowards(world, enemy.transformBody().getTranslation());

                    if (distanceToEnemy <= this.maxAttackDistance) {
                        // You can check if you can shoot before shooting
                        if (tank.canShoot(world)) {
                            // Or also just shoot, it will return false if you can't shoot.
                            // And by checking the world, if debug is enabled, you can print out a message.
                            if (tank.shoot(world) && world.isDebug()) {
                                System.out.println("Shot at enemy!");
                            }
                        }
                    }
                },
                () -> {
                    // No enemies found, move in a circle (negative is clockwise for yaw rotation)
                    tank.rotateBody(world, -myTankConfig.bodyRotationSpeed());
                    tank.move(world, Tank.MoveDirection.FORWARD);
                }
        ); */

        /*// Example of moving and rotating the tank
        tank.rotateBody(world, -myTankConfig.bodyRotationSpeed());
        tank.rotateTurretYaw(world, myTankConfig.turretYawRotationSpeed());
        // for pitch rotation, positive is down
        // tank.rotateTurretPitch(world, -myTankConfig.turretPitchRotationSpeed());
        tank.move(world, Tank.MoveDirection.FORWARD);*/

        // Get messages of a specific type only
        List<MessageContainer> hitMessages = world.getIncomingMessages(Hit.class);
        for (MessageContainer message : hitMessages) {
            Hit gotHitMessageData = (Hit) message.getMessage();
            handleHittingTank(world, gotHitMessageData);
        }

        // Get all messages
        List<MessageContainer> messages = world.getIncomingMessages();
        for (MessageContainer message : messages) {
            MessageData data = message.getMessage();

            switch (data) {
                case SimpleTextMessage textMessage ->
                        System.out.println("Received text message:\n\t" + textMessage.message());
                case GotHit gotHitMessageData -> handleGettingHit(world, gotHitMessageData);
                case Hit _ -> {
                    // We already handled this message type above
                }
                default -> System.err.println("Received unknown message type: " + data.getClass().getSimpleName());
            }
        }

        // Sending a nice message to all team members (individually, you could also send a single message to full team)
        teamMembers
                .forEach(target -> world.send(new MessageContainer(
                        CLIENT.get(target.clientId()),
                        new SimpleTextMessage(
                                "Hello " + target.clientName() + " from " + config.getMyConfig().clientName() + "!"
                        )
                )));

        // Sending a less nice message to all enemy team members
        enemyTeamMembers
                .forEach(target -> world.send(new MessageContainer(
                        CLIENT.get(target.clientId()),
                        new SimpleTextMessage(
                                "You're going down, " + target.clientName() + "!"
                        )
                )));
    }

    private void handleHittingTank(PublicGameWorld world, Hit hitMessageData) {
        ConnectedClientConfig targetConfig = world.getConnectedClientConfig(hitMessageData.hitEntity()).orElseThrow();
        TankConfig targetTankConfig = targetConfig.getTankConfig(world);
        TankConfig myTankConfig = world.getTank().getConfig(world);
        float armorOnHitSide = targetTankConfig.armor().get(hitMessageData.hitSide());
        float myExpectedDamage = myTankConfig.projectileDamage();
        float dealtDamage = hitMessageData.damageDealt();
        ClientState targetState = targetConfig.getClientState(world);
        System.out.println("Hit " + targetConfig.clientName() + " on " + hitMessageData.hitSide() + " side!");
        // print out how the damage was calculated
        System.out.println("Dealt damage: " + dealtDamage + " = " + myExpectedDamage + " * (1 - " + armorOnHitSide + ")");
        System.out.println(targetConfig.clientName() + " health: " + targetState.currentHealth());
    }

    private void handleGettingHit(PublicGameWorld world, GotHit gotHitMessageData) {
        ConnectedClientConfig shooterConfig = world.getConnectedClientConfig(gotHitMessageData.shooterEntity()).orElseThrow();
        System.out.println("Got hit by " + shooterConfig.clientName() + " on " + gotHitMessageData.hitSide());
        System.out.println("Received " + gotHitMessageData.damageReceived() + " damage!");
        System.out.println("Current health: " + world.getMyState().currentHealth());

        if (world.getMyState().state() == ClientState.PlayerState.DEAD) {
            System.out.println("I died! killed by " + shooterConfig.clientName());
        }
    }
}