package com.wildex999.schematicbuilder.gui.elements;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.lwjgl.opengl.GL11;

import com.wildex999.utils.ModLog;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

/*
 * Draws a list of selectable entries that can be scrolled.
 * Only draw entries that are visible.
 * 
 * For list movement, just use getLower/higher key on the current
 * top element to move up/down the list.
 * 
 */

public class GuiList extends Gui {
	public int posX, posY;
	public int width, height;
	public boolean noUpdate; //Don't update when removing/adding entries. Use when batching.
	public boolean toggleEntries = true; //If true, entries can be selected and unselected
	
	public boolean renderListBackground = true;
	public boolean renderScrollbarBackground = true;
	
	protected int entryWidth = 220;
	public int entryHeight = 16;
	protected GuiScreen screen;
	
	//Use treemap for sorting and searching(subMap) while keeping log(N) lookup.
	//GuiListEntry has an internal linked list
	protected TreeMap<String, GuiListEntry> list;
	protected TreeMap<String, GuiListEntry> searchList; //sublist of list used when searching/filtering
	protected HashMap<String, HashSet<GuiListEntry>> tagList; //Tags and entries containing each tag
	
	protected String prevSearch; 
	protected boolean prevIgnoreCase;
	
	protected int scrollbarPosX, scrollbarPosY;
	protected int scrollbarWidth;
	protected int scrollbarAreaHeight; //Usually same as height
	protected int scrollStartY; //Y when starting to drag scrollbar
	
	protected GuiListEntry topEntry; //Top visible entry
	protected int topEntryIndex; //Index of top entry, used to calculate scroll
	
	protected int overflow; //Height above the set GuiList height
	protected int visibleEntries;
	
	protected GuiButton scrollBar;
	protected int yScroll; //List scroll.
	
	public GuiListEntry selectedEntry;
	public int selectedEntryIndex;
	
	public GuiList(GuiScreen screen, int x, int y, int width, int height) {
		list = new TreeMap<String, GuiListEntry>(String.CASE_INSENSITIVE_ORDER);
		searchList = new TreeMap<String, GuiListEntry>(String.CASE_INSENSITIVE_ORDER);
		tagList = new HashMap<String, HashSet<GuiListEntry>>();

		prevSearch = "";
		prevIgnoreCase = false;
		
		posX = x;
		posY = y;
		this.width = width;
		this.entryWidth = width;
		this.height = height;
		overflow = 0;
		visibleEntries = 0;
		this.screen = screen;
		
		update();
	}
	
	//Get size of whole list
	public int getListSize() {
		return list.size();
	}
	
	//Get the number of elements remaining after filtering/search
	public int getListSizeFiltered() {
		return getCurrentList().size();
	}
	
	public TreeMap<String, GuiListEntry> getCurrentList() {
		if(prevSearch.length() == 0)
			return list;
		else
			return searchList;
	}
	
	//The button must be created and given a ID in the parent GuiScreen.
	//We then manage it's position and size.
	public void setScrollbarButton(GuiButton button) {
		scrollBar = button;
		
		scrollBar.xPosition = posX + width + 5;
		scrollBar.yPosition = posY;
		
		scrollBar.width = 10;
		scrollBar.height = 20;
		
		recalculateScrollButtonHeight();
	}
	
	//From: http://stackoverflow.com/questions/86780/is-the-contains-method-in-java-lang-string-case-sensitive/25379180#25379180
	public static boolean containsIgnoreCase(String src, String what) {
	    final int length = what.length();
	    if (length == 0)
	        return true; // Empty string is contained

	    final char firstLo = Character.toLowerCase(what.charAt(0));
	    final char firstUp = Character.toUpperCase(what.charAt(0));

	    for (int i = src.length() - length; i >= 0; i--) {
	        // Quick check before calling the more expensive regionMatches() method:
	        final char ch = src.charAt(i);
	        if (ch != firstLo && ch != firstUp)
	            continue;

	        if (src.regionMatches(true, i, what, 0, length))
	            return true;
	    }

	    return false;
	}
	
	//Filter input to any entry whose name matches search
	//first entry if currently looking outside search area
	public void setSearchString(String search, boolean ignoreCase) {
		if(search.length() == 0)
		{
			searchList.clear();
			prevSearch = "";
			prevIgnoreCase = ignoreCase;
			
			if(list.size() == 0)
			{
				topEntry = null;
				selectedEntry = null;
			}
			else
			{
				//At this point we don't know how many entries are before our current top or Selected entry
				GuiListEntry currentTop = topEntry;
				if(currentTop != null)
				{
					//Find top to have a starting point and known index
					topEntry = list.firstEntry().getValue();
					topEntryIndex = 0;
					topEntryIndex = findEntryIndex(currentTop);
					setTopEntry(currentTop, topEntryIndex);
				}
				else
					setTopEntry(list.firstEntry().getValue(), 0);
				
				if(selectedEntry != null)
				{
					selectedEntryIndex = findEntryIndex(selectedEntry);
					setTopEntry(selectedEntry, selectedEntryIndex);
				}
			}
			return;
		}
		
		//TODO: Make the search happen in steps doing x entries each time or just use threads
		TreeMap<String, GuiListEntry> currentList = list;
		
		if(search.length() >= prevSearch.length() && prevSearch.equals(search.substring(0, search.length())) && ignoreCase == prevIgnoreCase)
			currentList = searchList; //Continue search on current filtered list
		else
			searchList.clear(); //Restart search on full list
		
		//First see if current selection is in search results
		if(selectedEntry != null)
		{
			boolean contains;
			if(ignoreCase)
				contains = containsIgnoreCase(selectedEntry.name, search);
			else
				contains = selectedEntry.name.contains(search);
			
			if(contains)
			{
				searchList.put(selectedEntry.name, selectedEntry);
				selectedEntryIndex = 0;
			}
			else if(selectedEntry != null)
			{
				selectedEntry.onUnselect();
				selectedEntry = null;
			}
		}

		Set<Entry<String, GuiListEntry>> listSet = currentList.entrySet();
		for(Map.Entry<String, GuiListEntry> entry : listSet)
		{
			String name = entry.getKey();
			
			boolean contains;
			if(ignoreCase)
				contains = containsIgnoreCase(name, search);
			else
				contains = name.contains(search);
			
			if(contains)
			{
				searchList.put(name, entry.getValue());
				
				if(selectedEntry != null)
				{
					if(name.compareToIgnoreCase(selectedEntry.name) < 0)
						selectedEntryIndex++;
				}
			}
		}
		
		if(selectedEntry != null)
			setTopEntry(selectedEntry, selectedEntryIndex);
		else if(searchList.size() > 0)
			setTopEntry(searchList.firstEntry().getValue(), 0);
		else
			setTopEntry(null, 0);
		
		prevSearch = search;
		prevIgnoreCase = ignoreCase;
	}
	
	public void update() {
		if(noUpdate)
			return;
		
		TreeMap<String, GuiListEntry> currentList = getCurrentList();
		
		overflow = (currentList.size() * entryHeight) - height;
		visibleEntries = (int)Math.ceil((double)height/(double)entryHeight);
		
		updateTop();
		updateScrollBar();
	}
	
	public GuiListEntry nextEntry(TreeMap<String, GuiListEntry> list, GuiListEntry currentEntry) {
		Entry<String, GuiListEntry> nextEntry = list.higherEntry(currentEntry.name);
		if(nextEntry == null)
			return null;
		return nextEntry.getValue();
	}
	
	public GuiListEntry previousEntry(TreeMap<String, GuiListEntry> list, GuiListEntry currentEntry) {
		Entry<String, GuiListEntry> prevEntry = list.lowerEntry(currentEntry.name);
		if(prevEntry == null)
			return null;
		return prevEntry.getValue();
	}
	
	public boolean addEntry(GuiListEntry entry) {
		String name = entry.name;
		
		TreeMap<String, GuiListEntry> currentList = getCurrentList();
		
		if(list.containsKey(name))
			return false;
		
		list.put(name, entry);
		entry.list = this;
		
		if(currentList == searchList && !entry.name.contains(prevSearch))
			return true;
		
		//Check if before current top entry(Keep track of top entry index)
		if(topEntry != null)
		{
			int compare = name.compareToIgnoreCase(topEntry.name);
			if(compare < 0) //if new entry is before
				topEntryIndex++;
		}
		else
			setTopEntry(entry, 0);
		//Check if before selected entry
		if(selectedEntry != null)
		{
			int compare = name.compareToIgnoreCase(selectedEntry.name);
			if(compare < 0)
				selectedEntryIndex++;
		}
		
		update();
		updateScrollBar();
		return true;
	}
	
	public boolean removeEntry(GuiListEntry entry) {
		String name = entry.name;
		
		TreeMap<String, GuiListEntry> currentList = getCurrentList();
		
		if(currentList != list)
			list.remove(name); //Remove from both lists
		entry = currentList.remove(name);
		if(entry == null)
			return false;
		
		if(entry == selectedEntry)
		{
			selectedEntry.onUnselect();
			selectedEntry = null;
		}
		
		if(topEntry != null)
		{
			//Are we removing the top entry?
			if(entry == topEntry)
			{
				boolean oldNoUpdate = noUpdate;
				noUpdate = true;
				GuiListEntry nextEntry = nextEntry(currentList, topEntry);
				if(nextEntry != null)
					setTopEntry(nextEntry, topEntryIndex);
				else
					setTopEntry(null, 0);
				noUpdate = oldNoUpdate;
			}
			else
			{
				//Check if removing entry before top entry
				int compare = name.compareToIgnoreCase(topEntry.name);
				if(compare < 0)
					topEntryIndex--;
			}
		}
		
		//Check if removing entry before selected entry
		if(selectedEntry != null)
		{
			int compare = name.compareToIgnoreCase(selectedEntry.name);
			if(compare < 0)
				selectedEntryIndex--;
		}
		
		update();
		updateScrollBar();
		
		return true;
	}
	
	public boolean renameEntry(GuiListEntry entry, String newName) {
		if(!removeEntry(entry))
			return false;
		
		entry.name = newName;
		
		if(!addEntry(entry))
			return false;
		
		return true;
	}
	
	public void setSelectedEntry(GuiListEntry entry) {
		if(!list.containsKey(entry.name))
			return;
		
		int index = findEntryIndex(entry);
		setSelectedEntry(entry, index);
	}
	
	protected void setSelectedEntry(GuiListEntry entry, int entryIndex) {
		if(selectedEntry != null)
			selectedEntry.onUnselect();
		
		selectedEntry = entry;
		
		if(selectedEntry != null)
		{
			selectedEntryIndex = entryIndex;
			selectedEntry.onSelect();
		}
			
	}
	
	public int getSelectedEntryY() {
		if(selectedEntry == null)
			return -1;
		return posY + (selectedEntryIndex*entryHeight) - yScroll;
	}
	
	
	//Set the current top entry and it's index in the list.
	public void setTopEntry(GuiListEntry entry, int index) {
		topEntry = entry;
		topEntryIndex = index;
		scrollToIndex(topEntryIndex);
		
		update();
		
	}
	
	//Set top entry to current top entry - n entries.
	//Return how far up it moved(<n if reached beginning)
	public int topEntryMoveUp(int n) {
		if(topEntry == null)
			return 0;
		
		TreeMap<String, GuiListEntry> currentList = getCurrentList();
		
		GuiListEntry current = topEntry;
		GuiListEntry prevEntry;
		
		while(n-- > 0)
		{
			prevEntry = previousEntry(currentList, current);
			if(prevEntry == null)
				break;
			
			current = prevEntry;
			topEntryIndex--;
		}
		
		topEntry = current;
		if(topEntryIndex < 0)
			topEntryIndex = 0;
		
		return n;
	}
	
	//Move the top entry down to the next entry
	//Return how far down it moved(<n if reached end)
	public int topEntryMoveDown(int n) {
		if(topEntry == null)
			return 0;
		
		TreeMap<String, GuiListEntry> currentList = getCurrentList();
		
		GuiListEntry current = topEntry;
		GuiListEntry nextEntry;
		
		while(n-- > 0)
		{
			nextEntry = nextEntry(currentList, current);
			if(nextEntry == null)
				break;
			
			current = nextEntry;
			topEntryIndex++;
		}
		
		topEntry = current;
		if(topEntryIndex >= list.size())
			topEntryIndex = list.size() -1;
		
		return n;
	}
	
	//Draw the visible entries in the list
	public void draw(Minecraft mc) {
		//Start with top entry, iterate down until outside bounds
		
		ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
		int scale = sr.getScaleFactor();
		
		if(renderScrollbarBackground && scrollBar != null)
			this.drawRect(scrollBar.xPosition, posY, scrollBar.xPosition+scrollBar.width, posY+height, 0xFF000000);
		
		if(renderListBackground)
			this.drawRect(posX, posY, posX+width, posY+height, 0xFF000000);
		
		if(topEntry == null)
			return;
		
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor(posX*scale, (mc.currentScreen.height-(posY+height))*scale, width*scale, height*scale);
		
		TreeMap<String, GuiListEntry> currentList = getCurrentList();
		
		int drawCount = 0;
		GuiListEntry current = topEntry;
		while(current != null && drawCount < visibleEntries+1)
		{
			int drawOffset = (topEntryIndex+drawCount)*entryHeight - yScroll;
			
			current.draw(mc, posX, posY + drawOffset, entryWidth, entryHeight, false);
			drawCount++;
			
			current = nextEntry(currentList, current);
		}
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
	}
	
	public GuiListEntry getEntry(int index) {
		if(topEntry == null)
			return null;
		
		GuiListEntry current = topEntry;
		int currentIndex = topEntryIndex;
		
		TreeMap<String, GuiListEntry> currentList = getCurrentList();
		
		while(currentIndex < index)
		{
			current = nextEntry(currentList, current);
			currentIndex++;
			if(current == null)
				return null;
		}
		while(currentIndex > index)
		{
			current = previousEntry(currentList, current);
			currentIndex--;
			if(current == null)
				return null;
		}
		
		return current;
	}
	
	//Get the entry with the given name, or null if it does not exist
	public GuiListEntry getEntry(String name) {
		return list.get(name);
	}
	
	//Get the index of the given entry
	public int findEntryIndex(GuiListEntry entry) {
		if(entry == topEntry)
			return topEntryIndex;
		
		//We don't know the index, so we have to iterate the list starting from current top.
		int compare = entry.name.compareToIgnoreCase(topEntry.name);
		GuiListEntry current = topEntry;
		int currentIndex = topEntryIndex;
		
		//Two while's instead of one with if inside, dat branching.
		if(compare < 0)
		{
			while(current != null)
			{
				current = previousEntry(getCurrentList(), current);
				currentIndex--;
				if(current == entry)
					return currentIndex;
			}
		} else {
			while(current != null)
			{
				current = nextEntry(getCurrentList(), current);
				currentIndex++;
				if(current == entry)
					return currentIndex;
			}
		}
		//Assert: This should not happen. This means the entry is not in the linked list!
		return -1;
	}
	
	//Calculate and set the height og the scrollbar button.
	//Depends on the List element size, and number of entries.
	public void recalculateScrollButtonHeight() {
		if(scrollBar == null)
			return;
		
		TreeMap<String, GuiListEntry> currentList = getCurrentList();
		
		float ratio = (float)height / (float)(currentList.size()*entryHeight); 
		scrollBar.height = (int) Math.ceil(height * ratio);
		
		if(scrollBar.height > height)
			scrollBar.height = height;
		if(scrollBar.height < 3)
			scrollBar.height = 3;
		
	}
	
	//Scroll content to scrollbar position.
	//Called when scrollbar is moved by player.
	public void scrollToScrollbar() {
		//How much one unit of scrollbar movement equals
		float scrollFactor = (float)overflow / (float)(height - scrollBar.height);
		yScroll = (int) Math.round((scrollBar.yPosition - posY) * scrollFactor);
		
		updateTop();
	}
	
	//Update scrollbar position to point to current yScroll position.
	//Called when entries are added/removed, or when scrolling with scroll wheel.
	public void updateScrollBar() {
		recalculateScrollButtonHeight();
		
		if(scrollBar == null)
			return;
		
		TreeMap<String, GuiListEntry> currentList = getCurrentList();
		
		float scrolled =  (float)yScroll / (float)((entryHeight*currentList.size()) - height); //How much we have scrolled(0.0 to 1.0)
		scrollBar.yPosition = posY + (int)Math.ceil((height - scrollBar.height) * scrolled);
	}
	
	//Scroll to the currently selected entry
	public void scrollToSelected() {
		if(selectedEntry == null)
			return;
		
		TreeMap<String, GuiListEntry> currentList = getCurrentList();
		
		yScroll = selectedEntryIndex * entryHeight;
		
		if(yScroll > currentList.size()*entryHeight - height)
			yScroll = currentList.size()*entryHeight - height;
		if(yScroll < 0)
			yScroll = 0;
		
		updateTop();
		updateScrollBar();
	}
	
	public void scrollToIndex(int index) {
		TreeMap<String, GuiListEntry> currentList = getCurrentList();
		
		if(index < 0 || index >= currentList.size())
			return;
		
		yScroll = index * entryHeight;
		
		if(yScroll > currentList.size()*entryHeight - height)
			yScroll = currentList.size()*entryHeight - height;
		if(yScroll < 0)
			yScroll = 0;
		
		updateTop();
		updateScrollBar();
	}
	
	//Set the new Top Entry depending on yScroll
	public void updateTop() {
		int newEntryIndex = (int) Math.ceil(yScroll/entryHeight);
		
		//ModLog.logger.info("New Entry Index: " + newEntryIndex);
		TreeMap<String, GuiListEntry> currentList = getCurrentList();
		
		if(newEntryIndex >= currentList.size())
			newEntryIndex = currentList.size() -1;
		else if(newEntryIndex < 0)
			newEntryIndex = 0;
		
		if(newEntryIndex < topEntryIndex)
			topEntryMoveUp(topEntryIndex - newEntryIndex);
		else if(newEntryIndex > topEntryIndex)
			topEntryMoveDown(newEntryIndex - topEntryIndex);
	}
	
	//Clear all entries, and resetting any scroll, top entry and selection
	public void clear() {
		list.clear();
		searchList.clear();
		prevSearch = "";
		selectedEntry = null;
		topEntry = null;
		
		update();
	}
	
	//Called when mouse is clicked on GuiList.
	//Used to check which entry is currently selected
	//Return true if an entry was clicked
	public boolean onMouseClick(int x, int y) {
		TreeMap<String, GuiListEntry> currentList = getCurrentList();
		
		//Get entry which is clicked
		if(currentList.size() == 0)
			return false;
		
		int entryIndex = ((y + yScroll) - posY) / entryHeight;
		if(entryIndex < 0)
			entryIndex = 0;
		if(entryIndex >= currentList.size())
			entryIndex = currentList.size() - 1;
		
		GuiListEntry entry = getEntry(entryIndex);
		
		if(entry == null)
			return false;
		
		if(selectedEntry == entry && toggleEntries)
			setSelectedEntry(null, 0);
		else
			setSelectedEntry(entry, entryIndex);
		
		return true;
		
	}
	
	public void onButtonDragStart(GuiButton button, int x, int y) {
		if(scrollBar == null || button != scrollBar)
			return;
		
		scrollStartY = y;
	}
	
	public void onButtonDrag(GuiButton button, int x, int y) {
		if(scrollBar == null || button != scrollBar)
			return;
		if(y == scrollStartY)
			return;
		
		//scrollBar.yPosition = y - (scrollBar.height/2);
		scrollBar.yPosition += y - scrollStartY;
		scrollStartY = y;
		if(scrollBar.yPosition < posY)
			scrollBar.yPosition = posY;
		else if(scrollBar.yPosition + scrollBar.height > posY + height)
			scrollBar.yPosition = posY + height - scrollBar.height;
		
		scrollToScrollbar();
	}
	
	//Scroll list up/down
	public void onScroll(int scroll) {
		if(scroll > 0)
			yScroll -= entryHeight;
		else if(scroll < 0)
			yScroll += entryHeight;
		
		TreeMap<String, GuiListEntry> currentList = getCurrentList();
		
		if(yScroll < 0)
			yScroll = 0;
		else if(yScroll > (entryHeight*currentList.size()) - height)
			yScroll = (entryHeight*currentList.size()) - height;
		
		updateTop();
		updateScrollBar();
	}
	
}
