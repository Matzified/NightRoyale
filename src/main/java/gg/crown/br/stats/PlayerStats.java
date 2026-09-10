package gg.crown.br.stats;

public class PlayerStats {

    private int kills;
    private int wins;
    private int games;
    private int coins;
    private int xp;

    public PlayerStats() {}

    public PlayerStats(int kills, int wins, int games, int coins, int xp) {
        this.kills = kills;
        this.wins = wins;
        this.games = games;
        this.coins = coins;
        this.xp = xp;
    }

    public int getKills() { return kills; }
    public void addKill() { this.kills++; }

    public int getWins() { return wins; }
    public void addWin() { this.wins++; }

    public int getGames() { return games; }
    public void addGame() { this.games++; }

    public int getCoins() { return coins; }
    public void addCoins(int coins) { this.coins += coins; }

    public int getXp() { return xp; }
    public void addXp(int xp) { this.xp += xp; }

    public int getLevel() {
        return 1 + (xp / 1000);
    }

    public int getRating() {
        return (wins * 100) + (kills * 10) + (games * 2);
    }
}
