JSCH AppDynamics Agent Plugin
==================================

## Purpose ##

JSCH http://www.jcraft.com/jsch/
Is a Java Library used for SSH execution as well as file transfer. The goal of this Agent plugin is to instrument the SSH SFTP put and get transfers in appdynamics with Custom ExitCalls on the flowmap and ad SnapShot Custom Data to track what files are moving around, as well as tag any file transfer errors in active Business Transactions.
Please let us know in the Issues list if anything more is requested or something isn't working as expected.

## Required
- Agent version 21.02+
- Java 8


## Deployment steps
- Copy *AgentPlugin.jar file under <agent-install-dir>/ver.x.x.x.x/sdk-plugins

    
