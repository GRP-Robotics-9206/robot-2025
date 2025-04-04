// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import frc.robot.Constants.ArmConstants;
import frc.robot.Constants.OperatorConstants;
import frc.robot.subsystems.ArmSubsystem;
import frc.robot.subsystems.RollerSubsystem;
import frc.robot.subsystems.SwerveSubsystem;
import swervelib.SwerveInputStream;
import frc.robot.commands.AlgaeInCommand;
import frc.robot.commands.AlgaeOutCommand;
import frc.robot.commands.ArmDownCommand;
import frc.robot.commands.ArmUpCommand;
import frc.robot.commands.CoralOutCommand;
import frc.robot.commands.CoralStackCommand;

import com.reduxrobotics.canand.CanandEventLoop;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.auto.NamedCommands;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;

/**
 * This class is where the bulk of the robot should be declared. Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of the robot (including
 * subsystems, commands, and trigger mappings) should be declared here.
 */
public class RobotContainer {
  // The robot's subsystems and commands are defined here...

  // create a new swerve subsystem object
  public final SwerveSubsystem drivebase = new SwerveSubsystem();

  // other subsystems
  public final ArmSubsystem m_arm = new ArmSubsystem();
  public final RollerSubsystem m_roller = new RollerSubsystem();

  // auto chooser
  private final SendableChooser<Command> autoChooser; 

  // create an object for our driver controller
  // private final CommandXboxController driverController = new CommandXboxController(Constants.OperatorConstants.kDriverControllerPort);
  private final CommandXboxController driverController = new CommandXboxController(Constants.OperatorConstants.kDriverControllerPort);
  private final CommandXboxController operatorController = new CommandXboxController(Constants.OperatorConstants.kOperatorControllerPort);

  //private final SendableChooser<Command> autoChooser;
  // Build an auto chooser. This will use Commands.none() as the default option.

  /** The container for the robot. Contains subsystems, OI devices, and commands. */
  public RobotContainer() {

    CanandEventLoop.getInstance();
    // Configure the trigger bindings
    configureBindings();

    // Shut up
    DriverStation.silenceJoystickConnectionWarning(true);

    // set the default command for the drivebase to the drive command
    drivebase.setDefaultCommand(driveFieldOrientedAngularVelocity);

    // named commands for pathplanner
    NamedCommands.registerCommand("CoralOut", new CoralOutCommand(m_roller).withTimeout(0.4));
    NamedCommands.registerCommand("CoralStack", new CoralStackCommand(m_roller).withTimeout(1) );

    // Setup pathplaner auto chooser
    autoChooser = AutoBuilder.buildAutoChooser();

    SmartDashboard.putData("Auto Chooser", autoChooser);
  }

  // left stick controls the translation of the robot
  SwerveInputStream driveAngularVelocity = SwerveInputStream.of(drivebase.getSwerveDrive(), 
                                                                () -> driverController.getLeftY() * -1,
                                                                () -> driverController.getLeftX() * -1)
                                                                .withControllerRotationAxis(driverController::getRightX)
                                                                .deadband(OperatorConstants.DEADBAND)
                                                                .scaleTranslation(OperatorConstants.TRANSLATION_SCALE)
                                                                .scaleRotation(-OperatorConstants.ROTATION_SCALE)
                                                                .allianceRelativeControl(true);

  // For the right stick to correspond to the angle we want the robot to face instead of the speed of rotationa
  SwerveInputStream driveDirectAngle = driveAngularVelocity.copy().withControllerHeadingAxis(driverController::getRightX,
                                                                                             driverController::getRightY)
                                                                                             .headingWhile(true);
  

  // create a new command that calls the driveCommand that we made in the swerveSubsystem
  Command driveFieldOrientedAngularVelocity = drivebase.driveFieldOriented(driveAngularVelocity);

  // Same thing but for direct angle rather than angular velocity
  Command driveFieldOrientedDirectAngle     = drivebase.driveFieldOriented(driveDirectAngle);
  

  // define what buttons do on the controller
  private void configureBindings() {
    // swerve drive zero gyro
    // driverController.button(1).onTrue(drivebase.zeroGyro()); //zero the gyro when square(?) is pressed
    driverController.a().onTrue((Commands.runOnce(drivebase::zeroGyro)));

    // algie arm controls
    operatorController.leftBumper().whileTrue(new ArmUpCommand(m_arm));
    operatorController.leftTrigger(.6).whileTrue(new ArmDownCommand(m_arm));

    // algie in/out controls
    operatorController.rightBumper().whileTrue(new AlgaeInCommand(m_roller));
    operatorController.rightTrigger(.6).whileTrue(new AlgaeOutCommand(m_roller));

    // auto algie
    operatorController.a().whileTrue(new ArmDownCommand(m_arm).until(m_arm::isArmUp).andThen(new AlgaeInCommand(m_roller)));
    operatorController.a().whileFalse(new ParallelCommandGroup(new ArmUpCommand(m_arm), new AlgaeInCommand(m_roller)).until(m_arm::isArmAtBall));
    operatorController.b().onTrue(new AlgaeOutCommand(m_roller).withTimeout(0.3).andThen(new ArmUpCommand(m_arm).until(m_arm::isArmUp)));

    // zero the arm encoder
    driverController.x().onTrue((Commands.runOnce(m_arm::zeroEncoder)));

    // coral cotrols
    // If the secondary controller has x pressed the coral will be ejected with a slow rotation
    // If the secondary controller has y pressed the coral will be ejected with a fast rotation
    operatorController.x().whileTrue(new CoralOutCommand(m_roller));
    operatorController.y().whileTrue(new CoralStackCommand(m_roller));
  }

  public Command getPathPlannerAuto() {
    return autoChooser.getSelected();
  }

  public Command getAutonomousCommand() {
    return autoChooser.getSelected();
  }
}
