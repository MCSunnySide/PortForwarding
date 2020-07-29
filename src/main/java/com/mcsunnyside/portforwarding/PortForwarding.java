package com.mcsunnyside.portforwarding;

import com.google.common.base.Strings;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Timer;
import java.util.TimerTask;

public final class PortForwarding extends JavaPlugin {
    private Session session;
    private final JSch jSch = new JSch();
    private final Timer timer = new Timer();
    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        loadKey();
        connect();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(!isEnabled()){
                    return;
                }
                if(session == null || !session.isConnected()){
                    disconnect();
                    connect();
                }else{
                    try {
                        session.sendKeepAliveMsg();
                    } catch (Exception ignored) {
                    }
                }
            }
        }, 0, 5000);
    }

    private void connect(){
        try {
            session = jSch.getSession(getConfig().getString("ssh-usr"),getConfig().getString("ssh-host"),getConfig().getInt("ssh-port"));
            session.setHostKeyRepository(jSch.getHostKeyRepository());
            if(Strings.isNullOrEmpty(getConfig().getString("ssh-pwd"))){
                session.setPassword(getConfig().getString("ssh-pwd"));
            }
            getLogger().info("Connecting to remote SSH server...");
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(300000);
            if(!session.isConnected()){
                getLogger().warning("Failed connect to remote SSH server...");
                return;
            }else{
                getLogger().info("Successfully connect to remote SSH server!");
            }

            getLogger().info("Setting up forwarding...");
            getConfig().getStringList("rules").forEach((rule)->{
                //LOCAL PORT:REMOTE PORT
                String[] parameters = rule.split(":");
                if(parameters.length != 2){
                    getLogger().warning("Invalid rule: "+rule);
                    return;
                }
                try {
                    //session.setPortForwardingR(Integer.parseInt(parameters[1]),"localhost",Integer.parseInt(parameters[0]));
                    session.setPortForwardingR(null,Integer.parseInt(parameters[1]),"127.0.0.1",Integer.parseInt(parameters[0]));
                    getLogger().info("Successfully setup rule: "+rule);
                } catch (JSchException e) {
                    e.printStackTrace();
                    getLogger().warning("Failed to setup rule: "+rule);
                }
            });
        } catch (JSchException e) {
            e.printStackTrace();
        }
    }

    private void disconnect(){
        getConfig().getStringList("rules").forEach((rule)->{
            //LOCAL PORT:REMOTE PORT
            String[] parameters = rule.split(":");
            if(parameters.length != 2){
                getLogger().warning("Invalid rule: "+rule);
                return;
            }
            try {
                session.delPortForwardingR(Integer.parseInt(parameters[0]));
                session.delPortForwardingR(Integer.parseInt(parameters[1]));
            } catch (JSchException ignored) {
            }
        });
        session.disconnect();
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        disconnect();
        timer.cancel();
    }
    private boolean loaded = false;
    private void loadKey(){
        try {
            if(!loaded) {
                if(Strings.isNullOrEmpty(getConfig().getString("private-key"))) {
                    jSch.addIdentity(getConfig().getString("private-key"), getConfig().getString("passphrase"));
                    loaded = true;
                }
            }
        } catch (JSchException e) {
            e.printStackTrace();
        }
    }
}
