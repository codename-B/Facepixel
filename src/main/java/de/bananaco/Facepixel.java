package de.bananaco;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.hypixel.api.HypixelAPI;
import net.hypixel.api.reply.FriendsReply;
import net.hypixel.api.util.Callback;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;
import java.util.*;

@Mod(modid = Facepixel.MODID, version = Facepixel.VERSION, guiFactory = "de.bananaco.FacepixelGuiFactory")
public class Facepixel {

    public static final String MODID = "Facepixel";
    public static final String VERSION = "1.0";
    public static final String DEFAULT_KEY = "DEFAULT_KEY";

    public static File configFile;
    public static Configuration config;

    private static Map<String, Collection<String>> friendsCache = new HashMap<String, Collection<String>>();
    public static Map<UUID, String> stringCache = new HashMap<java.util.UUID, String>();

    public static UUID UUID;
    String clientUUID;

    boolean started = false;
    long waitUntil = System.currentTimeMillis();
    int updates = 0;

    public static Property getAPIKeyProperty() {
        return config.get(Configuration.CATEGORY_CLIENT, "API Key", DEFAULT_KEY, "Set your API key here or things won't work properly!");
    }

    public static String getAPIKey() {
        return getAPIKeyProperty().getString();
    }

    @EventHandler
    public void init(FMLPreInitializationEvent event) {
        configFile = event.getSuggestedConfigurationFile();
        config = new Configuration(configFile);

        config.load();
        getAPIKey();
        config.save();
    }

    @EventHandler
    public void init(FMLPostInitializationEvent event) {
        Minecraft minecraft = FMLClientHandler.instance().getClient();
        UUID = minecraft.getSession().getProfile().getId();
        clientUUID = UUID.toString().replaceAll("-", "");
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(new FacepixelRender());

        try {
            String key = getAPIKey();
            HypixelAPI.getInstance().setApiKey(UUID.fromString(key));

            started = true;
            updateFriends(clientUUID);
        } catch(Exception e) {
            e.printStackTrace();
            getAPIKeyProperty().set(DEFAULT_KEY);
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = false)
    public void tick(TickEvent.ClientTickEvent event) {
        // fire once per tick
        if(event.phase == TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getMinecraft();
        if(!mc.isGamePaused() && mc.thePlayer  != null && mc.theWorld != null) {
            if (getAPIKey().equals(DEFAULT_KEY)) {
                return;
            } else if (!started) {
                return;
            }

            if(System.currentTimeMillis() < waitUntil) {
                if(updates > 0) {
                    updates = 0;
                }
                return;
            }

            Collection<String> clientFriends = friendsCache.get(clientUUID);
            if (clientFriends.isEmpty()) return;

            for (EntityPlayer entityPlayer : mc.theWorld.playerEntities) {
                final String uuid = entityPlayer.getUniqueID().toString().replaceAll("-", "");
                if (clientFriends.contains(uuid)) {
                    stringCache.put(entityPlayer.getUniqueID(), "" + EnumChatFormatting.YELLOW + "You are friends!");
                    continue;
                }
                if (friendsCache.containsKey(uuid)) {
                    if(!stringCache.containsKey(entityPlayer.getUniqueID())) {
                        Collection<String> friends = friendsCache.get(uuid);
                        if (friends.isEmpty()) continue;
                        int inCommon = getMatches(friends, clientFriends);
                        if (inCommon == 0) {
                            stringCache.put(entityPlayer.getUniqueID(), null);
                        } else if (inCommon == 1) {
                            stringCache.put(entityPlayer.getUniqueID(), EnumChatFormatting.AQUA + String.valueOf(inCommon) + EnumChatFormatting.YELLOW + " friend in common!");
                        } else {
                            stringCache.put(entityPlayer.getUniqueID(), EnumChatFormatting.AQUA + String.valueOf(inCommon) + EnumChatFormatting.YELLOW + " friends in common!");
                        }
                    }
                } else {
                    updateFriends(uuid);
                }
            }
        }
    }

    private int getMatches(Collection<String> s1, Collection<String> s2) {
        int matches = 0;
        for(String st1 : s1) {
            for(String st2 : s2) {
                if(st1.equals(st2)) {
                    matches++;
                }
            }
        }
        return matches;
    }

    private void updateFriends(final String uuid) {
        if(updates >= 60) {
            waitUntil = System.currentTimeMillis() + 30 * 1000;
            return;
        }
        updates++;
        friendsCache.put(uuid, new HashSet<String>());
        HypixelAPI.getInstance().getFriendsByUUID(uuid, new Callback<FriendsReply>(FriendsReply.class) {
            @Override
            public void callback(Throwable failCause, FriendsReply result) {
                if(failCause != null) {
                    failCause.printStackTrace();
                    return;
                }
                JsonArray array = result.getRecords();
                Collection<String> friends = new HashSet<String>();
                for(int i=0; i<array.size(); i++) {
                    JsonElement el = array.get(i);
                    String s1 = el.getAsJsonObject().get("uuidSender").getAsString().toLowerCase();
                    String s2 = el.getAsJsonObject().get("uuidReceiver").getAsString().toLowerCase();
                    if(!s1.equals(uuid)) {
                        friends.add(s1);
                    }
                    if(!s2.equals(uuid)) {
                        friends.add(s2);
                    }
                }
                if(friends.isEmpty()) {
                    friends.add("");
                }
                friendsCache.put(uuid, friends);
            }
        });
    }

}
