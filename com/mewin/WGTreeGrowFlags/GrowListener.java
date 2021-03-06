/*
 * Copyright (C) 2012 mewin <mewin001@hotmail.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mewin.WGTreeGrowFlags;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import org.bukkit.ChatColor;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;

/**
 *
 * @author mewin <mewin001@hotmail.de>
 */
public class GrowListener implements Listener {
    private WorldGuardPlugin wgPlugin;
    
    public GrowListener(WorldGuardPlugin wgPlugin)
    {
        this.wgPlugin = wgPlugin;
    }
    
    @EventHandler
    public void onStructureGrow(StructureGrowEvent e)
    {
        RegionManager rm = wgPlugin.getRegionManager(e.getWorld());
        
        if (rm == null)
        {
            return;
        }
        
        ApplicableRegionSet regions = rm.getApplicableRegions(e.getLocation());
        Collection<ProtectedRegion> regCol = removeParents((Collection<ProtectedRegion>) getPrivateValue(regions, "applicable"));
        
        if (!regions.allows(WGTreeGrowFlagsPlugin.TREE_GROW_FLAG))
        {
            if (!e.isFromBonemeal() || !e.getPlayer().isOp())
            {
                e.setCancelled(true);
                if (e.getPlayer() != null)
                {
                    e.getPlayer().sendMessage(ChatColor.RED + "You may not grow trees here.");
                }
            }
        }
        
        Iterator<BlockState> itr = e.getBlocks().iterator();
        if (e.getPlayer() == null || !e.getPlayer().isOp())
        {
            while(itr.hasNext())
            {
                BlockState state = itr.next();

                Collection<ProtectedRegion> blockRegions = removeParents((Collection<ProtectedRegion>) getPrivateValue(rm.getApplicableRegions(state.getLocation()), "applicable"));
                Iterator<ProtectedRegion> itr2 = blockRegions.iterator();

                while (itr2.hasNext())
                {
                    ProtectedRegion region = itr2.next();
                    State flagState = region.getFlag(WGTreeGrowFlagsPlugin.LEAVE_GROW_FLAG);

                    if (flagState == State.DENY && !regCol.contains(region))
                    {
                        itr.remove();
                    }
                }
            }
        }
    }
    
    private HashSet<ProtectedRegion> removeParents(Collection<ProtectedRegion> pRegions)
    {
        HashSet<ProtectedRegion> regions = new HashSet<ProtectedRegion>(pRegions);
        HashSet<ProtectedRegion> regionsToRemove = new HashSet<ProtectedRegion>();
        
        for (ProtectedRegion region : regions)
        {
            ProtectedRegion parent = region.getParent();
            
            while(parent != null)
            {
                regionsToRemove.add(parent);
                
                parent = parent.getParent();
            }
        }
        
        for (ProtectedRegion parent : regionsToRemove)
        {
            regions.remove(parent);
        }
        
        return regions;
    }
    
    private Object getPrivateValue(Object obj, String name)
    {
        try
        {
            Field f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(obj);
        }
        catch(NoSuchFieldException ex)
        {
            return null;
        }
        catch(SecurityException ex)
        {
            return null;
        }
        catch(IllegalArgumentException ex)
        {
            return null;
        }
        catch(IllegalAccessException ex)
        {
            return null;
        }
    }
}
