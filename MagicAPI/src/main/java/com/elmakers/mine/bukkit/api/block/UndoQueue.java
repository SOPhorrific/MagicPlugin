package com.elmakers.mine.bukkit.api.block;

import com.elmakers.mine.bukkit.api.data.UndoData;
import org.bukkit.block.Block;

public interface UndoQueue {
    public void add(UndoList blocks);
    public UndoList getLast();
    public UndoList getLast(Block target);

    /**
     * Undo a recent construction performed by this Mage.
     *
     * This will restore anything changed by the last-cast
     * construction spell, and remove that construction from
     * the Mage's UndoQueue.
     *
     * It will skip undoing if the UndoList is older than
     * the specified timeout.
     *
     * @param timeout The maximum age of the change
     * @return The UndoList that was undone, or null if none.
     */
    public UndoList undoRecent(int timeout);
    public UndoList undoRecent(int timeout, String spellKey);

    /**
     * Undo a recent construction performed by this Mage against the
     * given Block
     *
     * This will restore anything changed by the last-cast
     * construction spell by this Mage that targeted the specific Block,
     * even if it was not the most recent Spell cast by that Mage.
     *
     * It will skip undoing if the UndoList is older than
     * the specified timeout.
     *
     * @param block The block to check for modifications.
     * @param timeout The maximum age of the change
     * @return The UndoList that was undone, or null if the Mage has no constructions for the given Block.
     */
    public UndoList undoRecent(Block block, int timeout);

    /**
     * Immediately undo any scheduled undo tasks.
     *
     * @return The number of tasks (spells) undone.
     */
    public int undoScheduled();

    /**
     * Get the current size of the queue.
     *
     * @return The queue depth.
     */
    public int getSize();

    public boolean isEmpty();
    public boolean commit();

    /**
     * Save the data in this undo queue
     */
    public void save(UndoData data);

    /**
     * Load the data in this undo queue
     */
    public void load(UndoData data);
}
