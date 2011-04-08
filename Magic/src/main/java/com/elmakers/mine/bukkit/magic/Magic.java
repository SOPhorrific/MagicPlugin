package com.elmakers.mine.bukkit.magic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.elmakers.mine.bukkit.magic.dao.SpellVariant;
import com.elmakers.mine.bukkit.persistence.Persistence;
import com.elmakers.mine.bukkit.persistence.dao.BlockList;
import com.elmakers.mine.bukkit.utilities.PluginUtilities;
import com.elmakers.mine.bukkit.utilities.UndoQueue;

public class Magic
{
    /*
     * Public API - Use for hooking up a plugin, or calling a spell
     */

    static final String                         DEFAULT_BUILDING_MATERIALS     = "1,2,3,4,5,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,24,25,35,41,42,43,45,46,47,48,49,56,57,60,65,66,73,74,79,80,81,82,83,85,86,87,88,89,90,91";
    static final String                         STICKY_MATERIALS               = "37,38,39,50,51,55,59,63,64,65,66,68,70,71,72,75,76,77,78,83";
    static final String                         STICKY_MATERIALS_DOUBLE_HEIGHT = "64,71,";

    private final boolean                       allowCommands                  = true;
    private final List<Material>                buildingMaterials              = new ArrayList<Material>();
    private final List<BlockList>               cleanupBlocks                  = new ArrayList<BlockList>();
    private final Object                        cleanupLock                    = new Object();

    private final List<Spell>                   deathListeners                 = new ArrayList<Spell>();
    private final List<Spell>                   damageListeners                = new ArrayList<Spell>();
    private final List<Spell>                   materialListeners              = new ArrayList<Spell>();
    private final List<Spell>                   movementListeners              = new ArrayList<Spell>();
    private final List<Spell>                   quitListeners                  = new ArrayList<Spell>();

    private long                                lastCleanupTime                = 0;
    private final Logger                        log                            = Logger.getLogger("Minecraft");

    protected Persistence                       persistence                    = null;
    protected Server                            server                         = null;
    protected PluginUtilities                   utilities                      = null;

    private final HashMap<String, PlayerSpells> playerSpells                   = new HashMap<String, PlayerSpells>();

    private final boolean                       quiet                          = true;
    private final boolean                       silent                         = false;

    private final HashMap<String, Spell>        spells                         = new HashMap<String, Spell>();

      /*
       * private boolean autoExpandUndo = true; 
       * private boolean autoPreventCaveIn = false; 
       * private int undoCaveInHeight = 32;
       // private Material gravityFillMaterial = Material.DIRT;
      */
    
    public boolean cast(Player player, SpellVariant spellVariant)
    {
        Spell base = null;
        
        return spellVariant.cast(base);
    }
    
    public boolean cast(Player player, String spellName)
    {
        SpellVariant spell = persistence.get(spellName, SpellVariant.class);
        return cast(player, spell);
    }
    
    public void addToUndoQueue(Player player, BlockList blocks)
    {
        UndoQueue queue = getUndoQueue(player.getName());

        
         // TODO: Get this working again! 
        
        /*
        if (autoExpandUndo)
        {
            BlockList expandedBlocks = new BlockList(blocks);
            for (UndoableBlock undoBlock : blocks.getBlocks())
            {
                Block block = undoBlock.getBlock();
                Material newType = block.getType();
                if (newType == undoBlock.getOriginalMaterial() || isSolid(newType))
                {
                    continue;
                }

                for (int side = 0; side < 4; side++)
                {
                    BlockFace sideFace = UndoableBlock.SIDES[side];
                    Block sideBlock = block.getFace(sideFace);
                    if (blocks.contains(sideBlock))
                        continue;

                    if (isSticky(undoBlock.getOriginalSideMaterial(side)))
                    {
                        UndoableBlock stickyBlock = expandedBlocks.addBlock(sideBlock);
                        stickyBlock.setFromSide(undoBlock, side);
                    }
                }

                Material topMaterial = undoBlock.getOriginalTopMaterial();
                Block topBlock = block.getFace(BlockFace.UP);
                if (!blocks.contains(topBlock))
                {
                    if (isAffectedByGravity(topMaterial))
                    {
                        expandedBlocks.addBlock(topBlock);
                        if (autoPreventCaveIn)
                        {
                            topBlock.setType(gravityFillMaterial);
                        }
                        else
                        {
                            for (int dy = 0; dy < undoCaveInHeight; dy++)
                            {
                                topBlock = topBlock.getFace(BlockFace.UP);
                                if (isAffectedByGravity(topBlock.getType()))
                                {
                                    expandedBlocks.addBlock(topBlock);
                                }
                                else
                                {
                                    break;
                                }
                            }
                        }
                    }
                    else if (isStickyAndTall(topMaterial))
                    {
                        UndoableBlock stickyBlock = expandedBlocks.addBlock(topBlock);
                        stickyBlock.setFromBottom(undoBlock);
                        stickyBlock = expandedBlocks.addBlock(topBlock.getFace(BlockFace.UP));
                        stickyBlock.setFromBottom(undoBlock);
                    }
                    else if (isSticky(topMaterial))
                    {
                        UndoableBlock stickyBlock = expandedBlocks.addBlock(topBlock);
                        stickyBlock.setFromBottom(undoBlock);
                    }
                }
            }
            blocks = expandedBlocks;
        }
        */
        
        queue.add(blocks);
    }

    public boolean allowCommandUse()
    {
        return allowCommands;
    }

    /*
     * Listeners / callbacks
     */

    public void cleanup()
    {
        synchronized (cleanupLock)
        {
            if (cleanupBlocks.size() == 0)
            {
                return;
            }

            List<BlockList> tempList = new ArrayList<BlockList>();
            tempList.addAll(cleanupBlocks);
            long timePassed = System.currentTimeMillis() - lastCleanupTime;
            for (BlockList blocks : tempList)
            {
                boolean undoSuccess = false;
                if (blocks.age((int) timePassed))
                {
                    undoSuccess = blocks.undo();
                }
                if (undoSuccess && blocks.isExpired())
                {
                    cleanupBlocks.remove(blocks);
                }
            }
            lastCleanupTime = System.currentTimeMillis();
        }
    }

    public void clear()
    {
        forceCleanup();
        movementListeners.clear();
        materialListeners.clear();
        quitListeners.clear();
        spells.clear();
    }

    public void forceCleanup()
    {
        for (BlockList blocks : cleanupBlocks)
        {
            blocks.undo();
        }
        cleanupBlocks.clear();
    }

    /*
     * public void setNether(NetherManager nether) { this.nether = nether; }
     */

    public List<Material> getBuildingMaterials()
    {
        return buildingMaterials;
    }

    public BlockList getLastBlockList(String playerName)
    {
        UndoQueue queue = getUndoQueue(playerName);
        return queue.getLast();
    }

    /*
     * Private data
     */

    public BlockList getLastBlockList(String playerName, Block target)
    {
        UndoQueue queue = getUndoQueue(playerName);
        return queue.getLast(target);
    }

    /*
     * Get the log, if you need to debug or log errors.
     */
    public Logger getLog()
    {
        return log;
    }
    
    public PlayerSpells getPlayerSpells(Player player)
    {
        PlayerSpells spells = playerSpells.get(player);
        if (spells == null)
        {
            spells = new PlayerSpells(player, this, persistence);
            playerSpells.put(player.getName(), spells);
        }
        
        return spells;
    }

    public UndoQueue getUndoQueue(String playerName)
    {
        Player player = server.getPlayer(playerName);
        if (player == null)
        {
            return null;
        }
        return getPlayerSpells(player).getUndoQueue();
    }

    public void initialize(Server server, Persistence persistence, PluginUtilities utilities)
    {
        this.server = server;
        this.persistence = persistence;
        this.utilities = utilities;
    }
    
    public void addSpells(List<Spell> moreSpells)
    {
        for (Spell spell : moreSpells)
        {
            if (spells.get(spell.getName()) != null)
            {
                log.warning("Magic: Duplicate spell name: " + spell.getName());
            }
            spells.put(spell.getName(), spell);
        }
    }

    public boolean isAffectedByGravity(Material mat)
    {
        // DOORS are on this list, it's a bit of a hack, but if you consider
        // them
        // as two separate blocks, the top one of which "breaks" when the bottom
        // one does,
        // it applies- but only really in the context of the auto-undo system,
        // so this should probably be its own mat list, ultimately.
        return mat == Material.GRAVEL || mat == Material.SAND || mat == Material.WOOD_DOOR || mat == Material.IRON_DOOR;
    }

    public boolean isQuiet()
    {
        return quiet;
    }

    public boolean isSilent()
    {
        return silent;
    }

    public boolean isSolid(Material mat)
    {
        return mat != Material.AIR && mat != Material.WATER && mat != Material.STATIONARY_WATER && mat != Material.LAVA && mat != Material.STATIONARY_LAVA;
    }

    protected void loadProperties()
    {
        /*
         * PluginProperties properties = new PluginProperties(propertiesFile);
         * properties.load();
         * 
         * undoQueueDepth = properties.getInteger("spells-general-undo-depth",
         * undoQueueDepth); silent =
         * properties.getBoolean("spells-general-silent", silent); quiet =
         * properties.getBoolean("spells-general-quiet", quiet); autoExpandUndo
         * = properties.getBoolean("spells-general-expand-undo",
         * autoExpandUndo); allowCommands =
         * properties.getBoolean("spells-general-allow-commands",
         * allowCommands); stickyMaterials =
         * PluginProperties.parseMaterials(STICKY_MATERIALS);
         * stickyMaterialsDoubleHeight =
         * PluginProperties.parseMaterials(STICKY_MATERIALS_DOUBLE_HEIGHT);
         * autoPreventCaveIn =
         * properties.getBoolean("spells-general-prevent-cavein",
         * autoPreventCaveIn); undoCaveInHeight =
         * properties.getInteger("spells-general-undo-cavein-height",
         * undoCaveInHeight);
         * 
         * //buildingMaterials =
         * properties.getMaterials("spells-general-building",
         * DEFAULT_BUILDING_MATERIALS); buildingMaterials =
         * PluginProperties.parseMaterials(DEFAULT_BUILDING_MATERIALS);
         * 
         * for (Spell spell : spells) { spell.onLoad(properties); }
         * 
         * properties.save();
         */
    }

    public void onPlayerDamage(Player player, EntityDamageEvent event)
    {
        // Must allow listeners to remove themselves during the event!
        List<Spell> active = new ArrayList<Spell>();
        active.addAll(damageListeners);
        for (Spell listener : active)
        {
            listener.onPlayerDamage(event);
        }
    }

    public void onPlayerDeath(Player player, EntityDeathEvent event)
    {
        // Must allow listeners to remove themselves during the event!
        List<Spell> active = new ArrayList<Spell>();
        active.addAll(deathListeners);
        for (Spell listener : active)
        {
            listener.onPlayerDeath(event);
        }
    }

    public void onPlayerMove(PlayerMoveEvent event)
    {
        // Used as a refresh timer for now.. :(
        cleanup();

        // Must allow listeners to remove themselves during the event!
        List<Spell> active = new ArrayList<Spell>();
        active.addAll(movementListeners);
        for (Spell listener : active)
        {
            listener.onPlayerMove(event);
        }
    }

    public void onPlayerQuit(PlayerEvent event)
    {
        // Must allow listeners to remove themselves during the event!
        List<Spell> active = new ArrayList<Spell>();
        active.addAll(quitListeners);
        for (Spell listener : active)
        {
            listener.onPlayerQuit(event);
        }
    }

    public void registerEvent(SpellEventType type, Spell spell)
    {
        switch (type)
        {
            case PLAYER_DAMAGE:
                if (!damageListeners.contains(spell))
                {
                    damageListeners.add(spell);
                }
                break;
            case PLAYER_MOVE:
                if (!movementListeners.contains(spell))
                {
                    movementListeners.add(spell);
                }
                break;
            case MATERIAL_CHANGE:
                if (!materialListeners.contains(spell))
                {
                    materialListeners.add(spell);
                }
                break;
            case PLAYER_QUIT:
                if (!quitListeners.contains(spell))
                {
                    quitListeners.add(spell);
                }
                break;
            case PLAYER_DEATH:
                if (!deathListeners.contains(spell))
                {
                    deathListeners.add(spell);
                }
                break;
        }
    }

    public void scheduleCleanup(BlockList blocks)
    {
        synchronized (cleanupLock)
        {
            if (cleanupBlocks.size() == 0)
            {
                lastCleanupTime = System.currentTimeMillis();
            }
            cleanupBlocks.add(blocks);
        }
    }

    // private NetherManager nether = null;

    public boolean undo(String playerName)
    {
        UndoQueue queue = getUndoQueue(playerName);
        return queue.undo();
    }

    public boolean undo(String playerName, Block target)
    {
        UndoQueue queue = getUndoQueue(playerName);
        return queue.undo(target);
    }

    public void unregisterEvent(SpellEventType type, Spell spell)
    {
        switch (type)
        {
            case PLAYER_MOVE:
                movementListeners.remove(spell);
                break;
            case MATERIAL_CHANGE:
                materialListeners.remove(spell);
                break;
            case PLAYER_QUIT:
                quitListeners.remove(spell);
                break;
            case PLAYER_DEATH:
                deathListeners.remove(spell);
                break;
            case PLAYER_DAMAGE:
                damageListeners.remove(spell);
                break;
         }
    }
}
