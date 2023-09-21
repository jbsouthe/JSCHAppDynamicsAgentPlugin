package com.cisco.josouthe;

import com.appdynamics.agent.api.AppdynamicsAgent;
import com.appdynamics.agent.api.ExitCall;
import com.appdynamics.agent.api.ExitTypes;
import com.appdynamics.agent.api.Transaction;
import com.appdynamics.instrumentation.sdk.Rule;
import com.appdynamics.instrumentation.sdk.SDKClassMatchType;
import com.appdynamics.instrumentation.sdk.toolbox.reflection.IReflector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChannelSFTPInterceptor extends MyBaseInterceptor{

    IReflector getSession, getHost, getPort, getUserName;

    public ChannelSFTPInterceptor() {
        super();

        getSession = makeInvokeInstanceMethodReflector("getSession");
        getHost = makeInvokeInstanceMethodReflector("getHost"); //String
        getPort = makeInvokeInstanceMethodReflector("getPort"); //Integer
        getUserName = makeInvokeInstanceMethodReflector("getUserName"); //String
    }

    @Override
    public Object onMethodBegin(Object objectIntercepted, String className, String methodName, Object[] params) {
        Transaction transaction = AppdynamicsAgent.getTransaction();
        if( isFakeTransaction(transaction) ) return null;

        String source = (String) params[0];
        String destination = (String) params[1];
        Object sftpProgressMonitor = params[2];
        Integer mode = (Integer) params[3]; // 0=OVERWRITE, 1=RESUME, 2=APPEND
        Object session = getReflectiveObject(objectIntercepted, getSession);
        String remoteHostName = getReflectiveString(session, getHost, "UNKNOWN-HOST");
        Integer remotePort = getReflectiveInteger(session, getPort, 22);
        String remoteUserName = getReflectiveString(session, getUserName, "UNKNOWN-USER");
        String exitCallName = String.format("SFTP %s %s",methodName, remoteHostName);
        Map<String,String> map = new HashMap<String,String>();
        map.put("HOST", remoteHostName);
        map.put("PORT", String.valueOf(remotePort));
        map.put("USER", remoteUserName);
        map.put("VERB", methodName);
        ExitCall exitCall = transaction.startExitCall(map, exitCallName, ExitTypes.CUSTOM, false);
        String sftpCommandString = "UNKNOWN-COMMAND";
        if( methodName.equals("put") ) {
            sftpCommandString = String.format("sftp put %s %s:%s",source,remoteHostName,destination);
        } else if( methodName.equals("get") ) {
            sftpCommandString = String.format("sftp get %s:%s %s",remoteHostName,source,destination);
        }
        collectSnapshotData(transaction, "SFTP Command", sftpCommandString);
        return new State(transaction,exitCall, sftpCommandString);
    }

    @Override
    public void onMethodEnd(Object state, Object object, String className, String methodName, Object[] params, Throwable exception, Object returnVal) {
        if( state == null ) return;
        Transaction transaction = ((State)state).transaction;
        ExitCall exitCall = ((State)state).exitCall;
        String sftpCommandString = ((State)state).command;

        if( exception != null ) {
            transaction.markAsError(String.format("SFTP %s Exception: %s", sftpCommandString, exception));
        }
        exitCall.end();
    }

    @Override
    public List<Rule> initializeRules() {
        List<Rule> rules = new ArrayList<Rule>();

        rules.add(new Rule.Builder(
                "com.jcraft.jsch.ChannelSftp")
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                .methodMatchString("put")
                .withParams("java.lang.String", "java.lang.String", "com.jcraft.jsch.SftpProgressMonitor", "java.lang.Integer")
                .build()
        );

        rules.add(new Rule.Builder(
                "com.jcraft.jsch.ChannelSftp")
                .classMatchType(SDKClassMatchType.MATCHES_CLASS)
                .methodMatchString("get")
                .withParams("java.lang.String", "java.lang.String", "com.jcraft.jsch.SftpProgressMonitor", "java.lang.Integer", "java.lang.Long")
                .build()
        );
        return rules;
    }

    public class State {
        public State(Transaction t, ExitCall e, String command ) {
            this.transaction = t;
            this.exitCall = e;
            this.command = command;
        }
        public Transaction transaction;
        public ExitCall exitCall;
        public String command;
    }
}
