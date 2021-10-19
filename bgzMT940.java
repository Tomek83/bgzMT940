package parserBgz;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

class utils {
	public String getCurrentDate () {
		Date date = new Date();
		DateFormat dateFormat = new SimpleDateFormat("YYMMdd");
        String strDate = dateFormat.format(date);
        return strDate;
	}

	public String stripSlashes (String value) {
		return value.replaceAll("[\\\\/]+", "").trim();
	}

	public String parseNumerWyciagu (String value) {
		if (value.indexOf("/") >= 0) {
			value = value.substring(0, value.indexOf("/"));
		}
		return value.trim();
	}

	public String parseTransakcja (String value) {
		if (value.indexOf("/") >= 0) {
			value = value.substring(0, value.indexOf("/"));
		}
		return value.trim();
	}

	public String parseBankKontrahentaNrb (String value) {
		value = this.parseNumerKonta(value);
		return value.substring(2, 10).trim();
	}

	public String parseSkroconyNumerKonta (String value) {
		value = this.parseNumerKonta(value);
		return value.substring(10, value.length()).trim();
	}

	public String parseNumerKonta (String value) {
		return value.replaceAll("[^0-9]+", "").trim();
	}
}

class item {
	public String transaction;
	public Map transactionInfo;
}

class response {
	public String $20;
	public String $25;
	public String $28C;
	public String $60F;
	public String $62F;
	public ArrayList<String> $61 = new ArrayList<String>();
	public ArrayList<Map> $86 = new ArrayList<Map>();

	public int getItemSize () {
		return this.$61.size();
	}

	public item getItem (int index) {
		item item = new item();
		item.transaction = this.$61.get(index);
		item.transactionInfo = this.$86.get(index);
		return item;
	}
}

class entryCollection {
	private ArrayList<entryMT940> entriesMT940 = new ArrayList<entryMT940>();

	public void add (entryMT940 entry) {
		this.entriesMT940.add(entry);
	}

	public entryMT940 get (int index) {
		return this.entriesMT940.get(index);
	}

	public int size() {
		return this.entriesMT940.size();
	}
}

class entryMT940 {
	private String content = "";
	private String lines[];
	public response output = new response();

	public entryMT940 (String content) {
		this.content = content;
		this.lines = content.split("\n");
	}

	public void parseContent () {
		for (int i = 0; i < this.lines.length; i++) {
			if (this.lines[i].matches(":[0-9a-zA-Z]+:(.*)")) {
				String tag = this.lines[i].substring(1, this.lines[i].indexOf(":", 1));
				switch (tag) {
					case "20":
							this.output.$20 = this.getLineValue(this.lines[i]);
						break;
					case "25":
							this.output.$25 = this.getLineValue(this.lines[i]);
						break;
					case "28C":
							this.output.$28C = this.getLineValue(this.lines[i]);
						break;
					case "60F":
							this.output.$60F = this.getLineValue(this.lines[i]);
						break;
					case "62F":
							this.output.$62F = this.getLineValue(this.lines[i]);
						break;
					case "61":
							this.output.$61.add(this.getLineValue(this.lines[i]));
						break;
					case "86":
							Map<String, String> field = new HashMap<String, String>();
							String lines[] = this.readLinesValue(i).split("\\^");
							String code = lines[0].trim();
							field.put("code", code);
							for (int n = 1; n < lines.length; n++) {
								String key = lines[n].substring(0, 2).trim();
								String value = lines[n].substring(2, lines[n].length()).trim();
								field.put(key, value);
							}
							this.output.$86.add(field);
						break;
				}
			}
		}
	}

	private String getLineValue (String line) {
		return line.replaceAll(":[0-9a-zA-Z]+:", "").trim();
	}

	private String readLinesValue (int from) {
		String value = "";
		for (int i = from; i < this.lines.length; i++) {
			if (i != from && this.lines[i].matches(":[0-9a-zA-Z]+:(.*)")) {
				break;
			} else {
				value += this.lines[i].trim();
			}
		}
		return value.replaceAll(":[0-9a-zA-Z]+:", "").trim();
	}
}

public class bgzMT940 {

	public static String convertToTags(String value, String tags[]) {
		String retval = "";
		String[] parts = value.split("(?<=\\G...................................)");
		int maxLenght = (parts.length < tags.length) ? parts.length : tags.length;
		for (int i = 0; i < maxLenght; i++) {
			retval += ("<" + tags[i] + parts[i] + "\r\n");
		}
		return retval.trim();
	}

	public static void main (String[] args) {
		try
		{
			utils utils = new utils();
			BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "Cp1250"));
			String line = "";
			String entry = "";
			String entry20[];
			String transaction = "";
			BufferedWriter output = new BufferedWriter(new FileWriter(args[1]));
			ArrayList<entryCollection> entryCollection = new ArrayList<entryCollection>();
			while ((line = input.readLine()) != null)
				{
					if (line.matches("-")) {
						entryCollection entryEntity = new entryCollection();
						entry20 = entry.split(":20:1");
						for (int i = 0; i < entry20.length; i++) {
							entry20[i] = entry20[i].trim();
							if (!entry20[i].isEmpty()) {
								entryEntity.add(new entryMT940(entry20[i]));
							}
						}
						entryCollection.add(entryEntity);
						entry = "";
					} else {
						entry += line + "\n";
					}
				}
			for (int i = 0; i < entryCollection.size(); i++) {
				entry = "{";
				for (int j = 0; j < entryCollection.get(i).size(); j++) {
					entryMT940 entryMT940 = entryCollection.get(i).get(j);
					entryMT940.parseContent();
					for (int n = 0; n < entryMT940.output.getItemSize(); n++) {
						item item = entryMT940.output.getItem(n);
						String kontrahentAdres = "";
						String kontrahentNumerKonta = "";
						transaction += String.format(":61:%s\r\n%s\r\n:86:%s<00%s\r\n",
							utils.parseTransakcja(item.transaction),
							item.transactionInfo.get("00"),
							item.transactionInfo.get("code"),
							item.transactionInfo.get("00")
						);
						if (item.transactionInfo.get("27") != null) {
							if (item.transactionInfo.get("27") != null) kontrahentAdres += item.transactionInfo.get("27");
							if (item.transactionInfo.get("28") != null) kontrahentAdres += item.transactionInfo.get("28");
							if (item.transactionInfo.get("29") != null) kontrahentAdres += item.transactionInfo.get("29");
						}
						if (kontrahentAdres.isEmpty()) {
							if (item.transactionInfo.get("32") != null) kontrahentAdres += item.transactionInfo.get("32");
							if (item.transactionInfo.get("33") != null) kontrahentAdres += item.transactionInfo.get("33");
							if (item.transactionInfo.get("60") != null) kontrahentAdres += item.transactionInfo.get("60");
							if (item.transactionInfo.get("61") != null) kontrahentAdres += item.transactionInfo.get("61");
							if (item.transactionInfo.get("62") != null) kontrahentAdres += item.transactionInfo.get("62");
							if (item.transactionInfo.get("63") != null) kontrahentAdres += item.transactionInfo.get("63");
						}
						if (item.transactionInfo.get("38") != null) kontrahentNumerKonta = item.transactionInfo.get("38").toString();
						if (item.transactionInfo.get("31") != null && kontrahentNumerKonta.isEmpty()) kontrahentNumerKonta = item.transactionInfo.get("31").toString();
						if (item.transactionInfo.get("20") != null) transaction += "<20" + item.transactionInfo.get("20") + "\r\n";
						if (item.transactionInfo.get("21") != null) transaction += "<21" + item.transactionInfo.get("21") + "\r\n";
						if (item.transactionInfo.get("22") != null) transaction += "<22" + item.transactionInfo.get("22") + "\r\n";
						if (item.transactionInfo.get("23") != null) transaction += "<23" + item.transactionInfo.get("23") + "\r\n";
						if (item.transactionInfo.get("24") != null) transaction += "<24" + item.transactionInfo.get("24") + "\r\n";
						if (item.transactionInfo.get("25") != null) transaction += "<25" + item.transactionInfo.get("25") + "\r\n";
						if (item.transactionInfo.get("26") != null) transaction += "<26" + item.transactionInfo.get("26") + "\r\n";
						if (!kontrahentAdres.isEmpty()) transaction += convertToTags(kontrahentAdres, new String[]{"27", "28", "29", "60"}) + "\r\n";
						if (!kontrahentNumerKonta.isEmpty()) {
							transaction += "<30" + utils.parseBankKontrahentaNrb(kontrahentNumerKonta) + "\r\n";
							transaction += "<31" + utils.parseSkroconyNumerKonta(kontrahentNumerKonta) + "\r\n";
							transaction += "<38" + utils.parseNumerKonta(kontrahentNumerKonta) + "\r\n";
						}
						if (item.transactionInfo.get("32") != null) transaction += "<32" + item.transactionInfo.get("32") + "\r\n";
						if (item.transactionInfo.get("33") != null) transaction += "<33" + item.transactionInfo.get("33") + "\r\n";
					}
					entry += String.format(":20:%s\r\n:25:%s\r\n:28C:%s\r\n:60F:%s\r\n%s\r\n:62F:%s\r\n",
						utils.getCurrentDate(),
						utils.stripSlashes(entryMT940.output.$25),
						utils.parseNumerWyciagu(entryMT940.output.$28C),
						entryMT940.output.$60F,
						transaction.trim(),
						entryMT940.output.$62F
					);
					transaction = "";
				}
				entry += "-}\r\n";
				output.write(entry);
			}
			output.close();
		}
		catch (IOException err) {
			err.printStackTrace();
			System.exit(1);
		}
		finally {
			System.out.println("Wynik zapisano w " + args[1]);
		}
	}
}