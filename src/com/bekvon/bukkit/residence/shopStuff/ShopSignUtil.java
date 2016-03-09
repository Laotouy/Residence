package com.bekvon.bukkit.residence.shopStuff;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import com.bekvon.bukkit.residence.CommentedYamlConfiguration;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;

public class ShopSignUtil {

    ConcurrentHashMap<String, List<ShopVote>> VoteList = new ConcurrentHashMap<String, List<ShopVote>>();
    List<Board> AllBoards = new ArrayList<Board>();

    private Residence plugin;

    public ShopSignUtil(Residence plugin) {
	this.plugin = plugin;
    }

    public void setVoteList(ConcurrentHashMap<String, List<ShopVote>> VoteList) {
	this.VoteList = VoteList;
    }

    public ConcurrentHashMap<String, List<ShopVote>> GetAllVoteList() {
	return VoteList;
    }

    public void removeVoteList(String resName) {
	VoteList.remove(resName);
    }

    public void addVote(String ResName, List<ShopVote> ShopVote) {
	VoteList.put(ResName, ShopVote);
    }

    public void setAllSigns(List<Board> AllBoards) {
	this.AllBoards = AllBoards;
    }

    public List<Board> GetAllBoards() {
	return AllBoards;
    }

    public void removeBoard(Board Board) {
	AllBoards.remove(Board);
    }

    public void addBoard(Board Board) {
	AllBoards.add(Board);
    }

    // Res Shop vote file
    public void LoadShopVotes() {
	GetAllVoteList().clear();
	File file = new File(plugin.getDataFolder(), "ShopVotes.yml");
	YamlConfiguration f = YamlConfiguration.loadConfiguration(file);

	if (!f.isConfigurationSection("ShopVotes"))
	    return;

	ConfigurationSection ConfCategory = f.getConfigurationSection("ShopVotes");
	ArrayList<String> categoriesList = new ArrayList<String>(ConfCategory.getKeys(false));
	if (categoriesList.size() == 0)
	    return;

	for (String category : categoriesList) {
	    List<String> List = ConfCategory.getStringList(category);
	    List<ShopVote> VoteList = new ArrayList<ShopVote>();
	    for (String oneEntry : List) {
		if (!oneEntry.contains("%"))
		    continue;

		String name = oneEntry.split("%")[0];
		int vote = -1;

		try {
		    String voteString = oneEntry.split("%")[1];
		    if (voteString.contains("!")) {
			voteString = oneEntry.split("%")[1].split("!")[0];
		    }
		    vote = Integer.parseInt(voteString);
		} catch (Exception ex) {
		    continue;
		}
		if (vote < 0)
		    vote = 0;
		else if (vote > 10)
		    vote = 10;

		long time = 0L;

		if (oneEntry.contains("!"))
		    try {
			time = Long.parseLong(oneEntry.split("!")[1]);
		    } catch (Exception ex) {
			time = System.currentTimeMillis();
		    }

		VoteList.add(new ShopVote(name, vote, time));

	    }
	    addVote(category, VoteList);
	}
	return;
    }

    // Signs save file
    public void saveShopVotes() {
	File f = new File(plugin.getDataFolder(), "ShopVotes.yml");
	YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);

	CommentedYamlConfiguration writer = new CommentedYamlConfiguration();
	conf.options().copyDefaults(true);

	writer.addComment("ShopVotes", "DO NOT EDIT THIS FILE BY HAND!");

	if (!conf.isConfigurationSection("ShopVotes"))
	    conf.createSection("ShopVotes");

	for (Entry<String, List<ShopVote>> one : GetAllVoteList().entrySet()) {

	    if (one.getKey() == null || one.getKey().equalsIgnoreCase(""))
		continue;

	    String path = "ShopVotes." + one.getKey();

	    List<String> list = new ArrayList<String>();

	    for (ShopVote oneVote : one.getValue()) {
		list.add(oneVote.getName() + "%" + oneVote.getVote() + "!" + oneVote.getTime());
	    }
	    writer.set(path, list);
	}

	try {
	    writer.save(f);
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return;
    }

    // Res Shop vote file
    public Vote getAverageVote(String resName) {

	ConcurrentHashMap<String, List<ShopVote>> allvotes = GetAllVoteList();

	if (!allvotes.containsKey(resName))
	    return new Vote(Residence.getConfigManager().getVoteRangeTo() / 2, 0);

	List<ShopVote> votes = allvotes.get(resName);

	double total = 0;
	for (ShopVote oneVote : votes) {
	    total += oneVote.getVote();
	}

	double vote = ((int) ((total / votes.size()) * 100)) / 100.0;

	return new Vote(vote, votes.size());
    }

    // Res Shop vote file
    public int getLikes(String resName) {
	ConcurrentHashMap<String, List<ShopVote>> allvotes = GetAllVoteList();
	if (!allvotes.containsKey(resName))
	    return 0;

	List<ShopVote> votes = allvotes.get(resName);

	int likes = 0;
	for (ShopVote oneVote : votes) {
	    if (oneVote.getVote() >= (int) (Residence.getConfigManager().getVoteRangeTo() / 2))
		likes++;
	}

	return likes;
    }

    public Map<String, Double> getSortedShopList() {

	Map<String, Double> allvotes = new HashMap<String, Double>();

	List<String> shops = Residence.getResidenceManager().getShops();

	for (String one : shops) {
	    if (Residence.getConfigManager().isOnlyLike())
		allvotes.put(one, (double) getLikes(one));
	    else
		allvotes.put(one, getAverageVote(one).getVote());
	}

	allvotes = sortByComparator(allvotes);

	return allvotes;
    }

    private Map<String, Double> sortByComparator(Map<String, Double> allvotes) {

	List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(allvotes.entrySet());

	Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
	    public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
		return (o2.getValue()).compareTo(o1.getValue());
	    }
	});
	Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
	for (Iterator<Map.Entry<String, Double>> it = list.iterator(); it.hasNext();) {
	    Map.Entry<String, Double> entry = it.next();
	    sortedMap.put(entry.getKey(), entry.getValue());
	}
	return sortedMap;
    }

    // Shop Sign file
    public void LoadSigns() {
	GetAllBoards().clear();
	File file = new File(plugin.getDataFolder(), "ShopSigns.yml");
	YamlConfiguration f = YamlConfiguration.loadConfiguration(file);

	if (!f.isConfigurationSection("ShopSigns"))
	    return;

	ConfigurationSection ConfCategory = f.getConfigurationSection("ShopSigns");
	ArrayList<String> categoriesList = new ArrayList<String>(ConfCategory.getKeys(false));
	if (categoriesList.size() == 0)
	    return;
	for (String category : categoriesList) {
	    ConfigurationSection NameSection = ConfCategory.getConfigurationSection(category);
	    Board newTemp = new Board();
	    newTemp.setStartPlace(NameSection.getInt("StartPlace"));
	    newTemp.setWorld(NameSection.getString("World"));
	    newTemp.setTX(NameSection.getInt("TX"));
	    newTemp.setTY(NameSection.getInt("TY"));
	    newTemp.setTZ(NameSection.getInt("TZ"));
	    newTemp.setBX(NameSection.getInt("BX"));
	    newTemp.setBY(NameSection.getInt("BY"));
	    newTemp.setBZ(NameSection.getInt("BZ"));

	    newTemp.GetTopLocation();
	    newTemp.GetBottomLocation();

	    newTemp.GetLocations();

	    addBoard(newTemp);
	}
	return;
    }

    // Signs save file
    public void saveSigns() {
	File f = new File(plugin.getDataFolder(), "ShopSigns.yml");
	YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);

	CommentedYamlConfiguration writer = new CommentedYamlConfiguration();
	conf.options().copyDefaults(true);

	writer.addComment("ShopSigns", "DO NOT EDIT THIS FILE BY HAND!");

	if (!conf.isConfigurationSection("ShopSigns"))
	    conf.createSection("ShopSigns");

	int cat = 0;
	for (Board one : GetAllBoards()) {
	    cat++;
	    String path = "ShopSigns." + cat;
	    writer.set(path + ".StartPlace", one.GetStartPlace());
	    writer.set(path + ".World", one.GetWorld());
	    writer.set(path + ".TX", one.GetTopLocation().getBlockX());
	    writer.set(path + ".TY", one.GetTopLocation().getBlockY());
	    writer.set(path + ".TZ", one.GetTopLocation().getBlockZ());
	    writer.set(path + ".BX", one.GetBottomLocation().getBlockX());
	    writer.set(path + ".BY", one.GetBottomLocation().getBlockY());
	    writer.set(path + ".BZ", one.GetBottomLocation().getBlockZ());
	}

	try {
	    writer.save(f);
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return;
    }

    public boolean BoardUpdateDelayed() {
	Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
	    public void run() {
		BoardUpdate();
		return;
	    }
	}, 20L);
	return true;
    }

    public boolean BoardUpdate() {
	for (Board board : GetAllBoards()) {
	    board.clearSignLoc();
	    List<Location> SignsLocation = board.GetLocations();

	    ArrayList<String> ShopNames = new ArrayList<String>(getSortedShopList().keySet());

	    int Start = board.GetStartPlace();
	    for (Location OneSignLoc : SignsLocation) {

		Block block = OneSignLoc.getBlock();

		if (!(block.getState() instanceof Sign))
		    continue;

		String Shop = null;
		if (Residence.getResidenceManager().getShops().size() >= Start)
		    Shop = ShopNames.get(Start - 1);

		ClaimedResidence res = Residence.getResidenceManager().getByName(Shop);

		if (res == null)
		    continue;

		Sign sign = (Sign) block.getState();

		Vote vote = null;
		String votestat = "";
		if (Residence.getResidenceManager().getShops().size() >= Start) {
		    vote = getAverageVote(ShopNames.get(Start - 1));

		    if (Residence.getConfigManager().isOnlyLike()) {
			votestat = vote.getAmount() == 0 ? "" : Residence.getLM().getMessage("Shop.ListLiked", getLikes(ShopNames.get(Start - 1)));
		    } else
			votestat = vote.getAmount() == 0 ? "" : Residence.getLM().getMessage("Shop.SignLines.4", vote.getVote() + "%" + vote.getAmount());
		}

		if (Shop != null) {
		    sign.setLine(0, Residence.getLM().getMessage("Shop.SignLines.1", String.valueOf(Start)));
		    sign.setLine(1, Residence.getLM().getMessage("Shop.SignLines.2", res.getName()));
		    sign.setLine(2, Residence.getLM().getMessage("Shop.SignLines.3", res.getOwner()));
		    sign.setLine(3, votestat);
		    board.addSignLoc(res.getName(), sign.getLocation());
		} else {
		    sign.setLine(0, "");
		    sign.setLine(1, "");
		    sign.setLine(2, "");
		    sign.setLine(3, "");
		}
		sign.update();

		Start++;
	    }
	}

	return true;
    }
}
