package frc.robot.subsystems;

import com.revrobotics.RelativeEncoder;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkBase.ResetMode;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.config.SparkMaxConfig;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.ArmConstants;

public class ArmSubsystem extends SubsystemBase {
  private final SparkMax armMotor;
  private final RelativeEncoder encoder;

  /**
   * This subsytem that controls the arm.
   */
  public ArmSubsystem () {
    // Set up the arm motor as a brushless motor
    armMotor = new SparkMax(ArmConstants.ARM_MOTOR_ID, MotorType.kBrushless);

    // Gets the Encoder object from the motor
    encoder = armMotor.getEncoder();

    // Set can timeout. Because this project only sets parameters once on
    // construction, the timeout can be long without blocking robot operation. Code
    // which sets or gets parameters during operation may need a shorter timeout.
    armMotor.setCANTimeout(250);

    // zero encoder
    encoder.setPosition(0);

    // Create and apply configuration for arm motor. Voltage compensation helps
    // the arm behave the same as the battery
    // voltage dips. The current limit helps prevent breaker trips or burning out
    // the motor in the event the arm stalls.
    SparkMaxConfig armConfig = new SparkMaxConfig();
    armConfig.voltageCompensation(10);
    armConfig.smartCurrentLimit(ArmConstants.ARM_MOTOR_CURRENT_LIMIT);
    armConfig.idleMode(IdleMode.kBrake);
    armMotor.configure(armConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
  }

  @Override
  public void periodic() {
  }
  
  /** 
   * This is a method that makes the arm move at your desired speed
   *  Positive values make it spin forward and negative values spin it in reverse
   * 
   * @param speed motor speed from -1.0 to 1, with 0 stopping it
   */

  public void runArm(double speed){
    armMotor.set(speed);
  }

  public double getEncoderPos(){
    return encoder.getPosition();
  }

  public Command zeroEncoder(){
    return run( () -> {
      encoder.setPosition(0);
    });
  }

  public boolean isArmUp(){
    return getEncoderPos() > ArmConstants.ARM_UP_OFFSET;
  }

  public boolean isArmAtBall(){
    return getEncoderPos() > ArmConstants.ARM_BALL_OFFSET;
  }

  public boolean isArmDown(){
    return getEncoderPos() < ArmConstants.ARM_DOWN_OFFSET;
  }
}