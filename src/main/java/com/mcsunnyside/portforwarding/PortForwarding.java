package com.mcsunnyside.portforwarding;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class PortForwarding extends JavaPlugin {
    private Session session;
    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        JSch jSch = new JSch();
        try {
            jSch.addIdentity(getConfig().getString("private-key"),getConfig().getString("passphrase"));
            session = jSch.getSession(getConfig().getString("ssh-usr"),getConfig().getString("ssh-host"),getConfig().getInt("ssh-port"));
            session.setHostKeyRepository(jSch.getHostKeyRepository());
            if(getConfig().getString("ssh-pwd") != null){
                session.setPassword(getConfig().getString("ssh-pwd"));
            }
            getLogger().info("Connecting to remote SSH server...");
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
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
        new BukkitRunnable(){
            @Override
            public void run() {
                if(!session.isConnected()){
                    onDisable();
                    onEnable(); //Reconnect
                }else{
                    try {
                        session.sendKeepAliveMsg();
                    } catch (Exception ignored) {
                    }
                }
            }
        }.runTaskTimerAsynchronously(this,0,20);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
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
}
