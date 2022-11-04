package Baneblade40k;
import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.awt.geom.*;

/**
 * Sources:
 * API https://robocode.sourceforge.io/docs/robocode/robocode/Robot.html
 * https://robowiki.net/wiki/Main_Page
 * https://robowiki.net/wiki/Category:Super_Sample_Bots
 */

/**
 * Baneblade40k - a robot by Axel Maniac
 */
public class Baneblade40k extends AdvancedRobot
{
	private int moveDirection = 1;
	private int turnDirection = 1;
	private double enemyEnergy = 100;				// Starting enemy energy.
	private static final double RADAR_ARC = 2;		// Fixed radar scan angle.

	public void run() {
		setColors(Color.black, Color.red, Color.black, Color.red, Color.green);		// body, gun, radar, bullet, scan colors.
		initializeRadar();	

		// Robot main loop
		while(true) {
			scan();
		}
	}

	/**
	 * onScannedRobot event.
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		performRadarScan(e);
		performMovement(e);
		performTargeting(e);
	}
	
	/**
	 * Radar code.
	 */
	public void initializeRadar(){
		// Set the body, gun, and radar to rotate independently.
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
		
		// Start turning the radar infinitely until a target is aquired.
		turnRadarRightRadians(Double.POSITIVE_INFINITY);
	}	

	public void performRadarScan(ScannedRobotEvent e) {	
		double enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();								// Enemy robot absolute bearing.
		double radarTurnAngle = Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians());		// Angle tu turn the radar towards the enemy.
		setTurnRadarRightRadians(radarTurnAngle * RADAR_ARC);													// Rotate radar to the right by the specified amount.
	}
	
	/**
	 *  Movement code. Simple movement. Bot tries to get in close, once under 150px to the enemy starts circling it perpedicularly. Also randomly changes turn and movement direction when enemy fires.
	 */
	public void performMovement(ScannedRobotEvent e) {
		double enemyAbsoluteBearing = e.getBearingRadians()+ getHeadingRadians();								// Enemy robot absolute bearing.
		double lateralVelocity = e.getVelocity() * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing);		// Enemy robot lateral velocity.

		// Increase our turn speed amount randomly each tick, between 4 and max turn rate allowed.
		double turnRate = 4;	
		turnRate += 0.2 * Math.random();
		if(turnRate > Rules.MAX_TURN_RATE){
			turnRate = 4;
		}
		
		//we set our maximum speed to go down as our turn rate goes up so that when we turn slowly, we speed up and vice versa;
		setMaxTurnRate(turnRate);
		setMaxVelocity(12 - turnRate);
		enemyEnergy = e.getEnergy();

		if (e.getDistance() > 150) {
			avoidEnemyFire(e);																												// Detect incoming fire and try to randomly avoid the bullets.
			setTurnRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getHeadingRadians() + lateralVelocity / getVelocity())); 	// Turn the robot to a future position of the enemy based on lateral velocity.
			setAhead((e.getDistance() - 140) * moveDirection);																				// Move forward towards the enemy until under 150px;
		}
		else{
			avoidEnemyFire(e);										// Detect incoming fire and try to randomly avoid the bullets.
			setTurnLeft(-90 - e.getBearing()); 						// Turn perpendicular to the enemy.
			if (getTime() % 20 == 0) {								// Every 20 ticks change the direction we are circling the enemy.
				moveDirection = -moveDirection;
				setAhead((e.getDistance() - 140) * moveDirection);	// Start circling the enemy.
				moveDirection = -moveDirection;						// Reset the move direction.
			}
		}	
	}
	
	/**
	 * Check if enemy fired a bullet.
	 */
	public boolean enemyFired(ScannedRobotEvent e) {
		return enemyEnergy - e.getEnergy() <= 3 && enemyEnergy - e.getEnergy() >= 0.1;	// If tracked enemy energy value is between 0.1 and 3 they fired a bullet towards us.
	}
	
	/**
	 * Code red! Set evasive manouvers!
	 */
	public void avoidEnemyFire(ScannedRobotEvent e){
		if(enemyFired(e)){
			// Randomly change turn direction.
			if(Math.random() > .5){
				turnDirection = -turnDirection;
			}
			
			// Randomly change move direction.
			if(Math.random() > .8){
				moveDirection = -moveDirection;
			}
			
			// Randomly turn left or right.
			if(Math.random() > 0.7){
				setTurnLeft(90 * turnDirection);
			}
			else {
				setTurnRight(90 * turnDirection);
			}

			// Move ahead for a set distance.
			setAhead(90 * moveDirection);
			
			// Reset turn and move directions.
			turnDirection = 1;
			moveDirection = 1;
		}
	}
	
	/**
	 *  Targeting code. Uses linear targeting taking into account enemy lateral velocity.
	 */
	public void performTargeting(ScannedRobotEvent e) {
		double enemyAbsoluteBearing = e.getBearingRadians() + getHeadingRadians();									// Enemy robot absolute bearing.
		double enemyLateralVelocity = e.getVelocity() * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing);		// Enemy lateral velocity.
		
		// Default bot size is 36x36. Lead the shot by half the size of a bot.
		double leadingBulletAmount = 18;	
		
		// Rotate the gun to target using a leading shot.
		double gunTurnAmount = Utils.normalRelativeAngle(enemyAbsoluteBearing - getGunHeadingRadians() + enemyLateralVelocity / leadingBulletAmount);
		setTurnGunRightRadians(gunTurnAmount);
		
		// Set bullet power based on the distance. Use more power when closer to the target, up to 3.
		double firePower = Math.min(450 / e.getDistance(), 3);
		
		// To conserve energy, fire the gun if cool and only if less than 15 degrees of gun turn remaining.
		if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 15) {
			setFire(firePower);
		}
	}

	/**
	 * onHitWall event.
	 */
	public void onHitWall(HitWallEvent e){
		moveDirection = -moveDirection; 	// You hit the wall stupid! Back away!
	}
}
