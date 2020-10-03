package org.mtransit.parser.ca_milton_transit_bus;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://milton.tmix.se/gtfs/gtfs-milton.zip
public class MiltonTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-milton-transit-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new MiltonTransitBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating Milton Transit bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating Milton Transit bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if ("Not In Service".equalsIgnoreCase(gTrip.getTripHeadsign())) {
			return true;
		}
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public boolean excludeRoute(@NotNull GRoute gRoute) {
		return super.excludeRoute(gRoute);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static final String A = "A";
	private static final String B = "B";
	private static final String C = "C";

	private static final String EB = "EB";
	private static final String WB = "WB";
	private static final String AM = "AM";
	private static final String PM = "PM";

	private static final long ROUTE_ID_ENDS_WITH_A = 10_000L;
	private static final long ROUTE_ID_ENDS_WITH_B = 20_000L;
	private static final long ROUTE_ID_ENDS_WITH_C = 30_000L;

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		String rsn = gRoute.getRouteShortName();
		if (!rsn.isEmpty() && Utils.isDigitsOnly(rsn)) {
			return Long.parseLong(rsn); // use route short name as route ID
		}
		int indexOf;
		indexOf = rsn.indexOf(EB);
		if (indexOf >= 0) {
			return Long.parseLong(rsn.substring(0, indexOf)); // use route short name as route ID
		}
		indexOf = rsn.indexOf(WB);
		if (indexOf >= 0) {
			return Long.parseLong(rsn.substring(0, indexOf)); // use route short name as route ID
		}
		indexOf = rsn.indexOf(AM);
		if (indexOf >= 0) {
			return Long.parseLong(rsn.substring(0, indexOf)); // use route short name as route ID
		}
		indexOf = rsn.indexOf(PM);
		if (indexOf >= 0) {
			return Long.parseLong(rsn.substring(0, indexOf)); // use route short name as route ID
		}
		Matcher matcher = DIGITS.matcher(rsn);
		if (matcher.find()) {
			long id = Long.parseLong(matcher.group());
			if (rsn.endsWith(A)) {
				return ROUTE_ID_ENDS_WITH_A + id;
			} else if (rsn.endsWith(B)) {
				return ROUTE_ID_ENDS_WITH_B + id;
			} else if (rsn.endsWith(C)) {
				return ROUTE_ID_ENDS_WITH_C + id;
			}
		}
		if ("DMSF".equalsIgnoreCase(rsn)) {
			return 99_000L;
		}
		throw new MTLog.Fatal("Unexpected route ID for %s!", gRoute);
	}

	@Nullable
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		return gRoute.getRouteShortName();
	}

	private static final String ROUTE_COLOR_SCHOOL = "FFD800"; // School bus yellow

	@SuppressWarnings("DuplicateBranchesInSwitch")
	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
		String routeColor = gRoute.getRouteColor();
		if ("000000".equalsIgnoreCase(routeColor)) {
			routeColor = null;
		}
		if (StringUtils.isEmpty(routeColor)) {
			String rsn = gRoute.getRouteShortName();
			if (Utils.isDigitsOnly(rsn)) {
				switch (Integer.parseInt(rsn)) {
				case 1:
					return "EF2E31"; // TODO really?
				case 2:
					return "263D96";
				case 3:
					return "195B1C";
				case 4:
					return "EC008C";
				case 5:
					return "FBBF15";
				case 6:
					return "A32B9B";
				case 7:
					return "00ADEF";
				case 8:
					return "6EC72D";
				case 9:
					return "F57814";
				case 10:
					return "E6C617";
				case 30:
					return null; // TODO ?
				case 31:
					return null; // TODO ?
				case 32:
					return null; // TODO ?
				case 50:
					return ROUTE_COLOR_SCHOOL;
				case 51:
					return ROUTE_COLOR_SCHOOL;
				case 52:
					return ROUTE_COLOR_SCHOOL;
				}
			}
			if ("1A".equalsIgnoreCase(rsn)) {
				return "EF2E31";
			}
			if ("1B".equalsIgnoreCase(rsn)) {
				return "B4131D";
			}
			if ("1C".equalsIgnoreCase(rsn)) {
				return "AB6326";
			}
			if ("DMSF".equalsIgnoreCase(rsn)) {
				return null; // TODO
			}
			throw new MTLog.Fatal("Unexpected route color for '%s; !", gRoute.toStringPlus());
		}
		return routeColor;
	}

	@SuppressWarnings("DuplicateBranchesInSwitch")
	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		String rlnS = gRoute.getRouteLongName();
		if (!StringUtils.isEmpty(rlnS)) {
			return cleanRouteLongName(rlnS);
		}
		String rsnS = gRoute.getRouteShortName();
		if (Utils.isDigitsOnly(rsnS)) {
			int rsn = Integer.parseInt(rsnS);
			switch (rsn) {
			case 1:
				return "Industrial";
			case 2:
				return "Main";
			case 3:
				return "Trudeau";
			case 4:
				return "Thompson / Clark";
			case 5:
				return "Yates";
			case 6:
				return "Scott";
			case 7:
				return "Harrison";
			case 8:
				return "Willmott";
			case 9:
				return "Ontario South";
			case 10:
				return "Farmstead";
			case 30:
				return "Milton West Zone";
			case 31:
				return "Milton Central Zone";
			case 32:
				return "Milton East Zone";
			case 50:
				return "School Special";
			case 51:
				return "School Special";
			case 52:
				return "School Special";
			}
		}
		if ("1A".equalsIgnoreCase(rsnS)) {
			return "Industrial";
		}
		if ("1B".equalsIgnoreCase(rsnS)) {
			return "Industrial";
		}
		if ("1C".equalsIgnoreCase(rsnS)) {
			return "Industrial";
		}
		throw new MTLog.Fatal("Unexpected route long name for %s!", gRoute.toStringPlus());
	}

	private static final Pattern STARTS_WITH_RSN = Pattern.compile("(^[\\d]+[a-zA-Z]? (- )?)", Pattern.CASE_INSENSITIVE);

	private String cleanRouteLongName(String routeLongName) {
		if (Utils.isUppercaseOnly(routeLongName, true, true)) {
			routeLongName = routeLongName.toLowerCase(Locale.ENGLISH);
		}
		routeLongName = STARTS_WITH_RSN.matcher(routeLongName).replaceAll(StringUtils.EMPTY);
		routeLongName = CleanUtils.cleanBounds(routeLongName);
		return CleanUtils.cleanLabel(routeLongName);
	}

	private static final String AGENCY_COLOR_GREEN = "00615C"; // GREEN (like color on buses)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	private static final String MILTON_GO = "Milton Go";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;

	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		//noinspection deprecation
		map2.put(1L + ROUTE_ID_ENDS_WITH_B, new RouteTripSpec(1L + ROUTE_ID_ENDS_WITH_B, // 1B
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // RR 25
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // Milton GO
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"2351", // Milton GO Station
								"2075", // ++
								"2314", // ++ James Snow
								"2315", // ++ Regional Road 25 (James Snow)
								"2316" // No 5 Side (RR 25)
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"2316", // No 5 Side (RR 25)
								"2317", // ++ Regional Road 25 (Peddle Rd)
								"2318", // ++ Regional Road 25 (Peddle Rd)
								"2319", // ++ Regional Road 25 (Escarpment Way)
								"2320", // ++ James Snow
								"2335", // ++
								"2017", // ==
								"2351", // != Milton GO Station =>
								"2169" // != Milton GO Station =>
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(1L + ROUTE_ID_ENDS_WITH_C, new RouteTripSpec(1L + ROUTE_ID_ENDS_WITH_C, // 1C
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // High Pt
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Milton GO
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								"2203", // <> Milton GO Station
								"2354", // ++
								"2366", // !=
								"2333", // <> Highway 401 Park-And-Ride
								"2324", // != Regional Road 25
								"2326" // High Point (Parkhill Dr)
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								"2326", // High Point (Parkhill Dr)
								"2327", // Parkhill
								"2331", // High Point
								"2332", // != Regional Road 25
								"2333", // <> Highway 401 Park-And-Ride
								"2334", // !=
								"2347", // ++
								"2017", // ==
								"2203", // != <> Milton GO Station => CONTINUE
								"2169" // != Milton GO Station =>
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(3L, new RouteTripSpec(3L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // Milton GO
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Louis St Laurent
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								"2286", // Fourth Line (Louis St Laurent)
								"2287", // Stewart
								"2295", // Waldie
								"2296", // ==
								"2277", // !== <>
								"2278", // != <>
								"2045", // !== != Thomas =>
								"2297", // !=
								"2123" // != Milton GO Station =>
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								"2123", // !== Milton GO Station <=
								"2276", // != !=
								"2277", // != <>
								"2278", // !== <>
								"2045", // != Thomas <=
								"2202", // !=
								"2279", // ==
								"2283", // Earl
								"2285", // Louis St Laurent
								"2286" // Fourth Line (Louis St Laurent)
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(4L, new RouteTripSpec(4L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // Milton GO
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Armstrong // 4th Ln & Louis St Laurent
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								"2147", // Fourth Line (Louis St Laurent)
								"2159", // ++
								"2164", // ==
								"2237", // !==
								"3014", // != Bronte at Laurier
								"2405", // !== Commercial =>
								"2165", // !==
								"2168", // != ==
								"2123", // != != Milton GO Station => CONTINUE
								"2169" // !== != Milton GO Station => END
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								"2123", // != Milton GO Station <=
								"2127", // !=
								"2405", // != Commercial
								"2217", // !=
								"2128", // ==
								"2134", // ++
								"2139", // Armstrong (Ferguson)
								"2147" // Fourth Line (Louis St Laurent)
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(5L, new RouteTripSpec(5L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // Milton GO
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Hepburn
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								"2225", // Hepburn (Philbrook)
								"2228", // Yates (Hepburn)
								"2233", // Holly
								"2235", // ==
								"2173", // !=
								"2405", // != Commercial =>
								"2236", // !==
								"2168", // !=
								"2169" // !== <> Milton GO Station =>
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								"2169", // !== <> Milton GO Station <=
								"2124", // !=
								"2218", // !==
								"2405", // != Commercial <=
								"2200", // !=
								"2220", // ==
								"2224", // ==
								"3015", // !=
								"3016", // !=
								"2225", // ==
								"2225" // Hepburn (Philbrook)
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(6L, new RouteTripSpec(6L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // Milton GO
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // Derry
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"2094", // Derry (Savoline)
								"2096", // Hinchey
								"2108", // Main
								"2109", // ==
								"2030", // !==
								"2045", // !== Thomas =>
								"2052", // !== == Bronte
								"2054", // != Brown
								"2110", // != Fulton
								"2055", // != Mill
								"2057", // != TransCab Transfer Point Westbound
								"2058", // == Court
								"2203" // !== Milton GO Station =>
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"2203", // !== Milton GO Station
								"2024", // ==
								"2080", // !=
								"2029", // !==
								"2025", // !=
								"2028", // !==
								"2045", // !== Thomas <=
								"2051", // !==
								"2082", // ==
								"2083", // Main
								"2091", // Weston
								"2092", // == Derry
								"2180", // !=
								"2190", // !=
								"2093", // != Scott
								"2094" // == Derry (Savoline)
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(7L, new RouteTripSpec(7L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // Milton GO
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // Savoline
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"2191", // Savoline (Derry)
								"2240", // ++
								"2203" // Milton GO Station
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"2203", // Milton GO Station
								"2180", // Scott at Derry
								"2185", // Dymott at Savoline
								"2191" // Savoline (Derry)
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(8L, new RouteTripSpec(8L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.EAST.getId(), // Milton GO
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.WEST.getId()) // Ruhl
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
								"2254", // Louis St Laurent (Bronte)
								"2259", // Ruhl (Farmstead)
								"2264", // == Milton Sports Centre
								"2176", // !== <>
								"2177", // <>
								"2178", // <>
								"2431", // !=
								"2405", // !== Commercial =>
								"2195", // !==
								"2200", // ++
								"2169" // !≃= Milton GO Station =>
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
								"2169", // !== Milton GO Station <=
								"2175", // !== Ontario
								"2405", // !== Commercial <=
								"2400", // !==
								"2176", // == <>
								"2177", // == <>
								"2178", // == <>
								"2246", // == !=
								"2250", // Ruhl (Bronte)
								"2254"  // Louis St Laurent (Bronte)
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(9L, new RouteTripSpec(9L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // Milton GO
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Britannia
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								"2392", // == Farmstead (Etheridge)
								"3009", // !==
								"2426", // Milton Marketplaces - Bank of Montreal
								"2405", // !== Commercial =>
								"2393", // !== Britannia (Farmstead)
								"2394", // RR 25 at Louis St Laurent (SE)
								"2017", // ==
								"2169", // !== <> Milton GO Station => SOUTH
								"2123", // !== Milton GO Station =>
								"2203" // !== Milton GO Station =>
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								"2169", // !== <> Milton GO Station <= NORTH
								"2245", // !==
								"2405", // !== Commercial <=
								"3013", // !==
								"2390", // ==
								"2391", // Etheridge at Orr
								"2392" // == Farmstead (Etheridge)
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(10L, new RouteTripSpec(10L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // Milton GO
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Britannia
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								"2418", // == Etheridge
								"3009", // != Etheridge at Connaught
								"3012", // != Etheridge at Chretien
								"2419", // != Bronson, across from Belmore
								"2421", // != Tasker
								"2422", // == alongside Holbrook =>
								"2420", // Britannia
								"2052", // Bronte
								"2054", // ==
								"2055", // !=
								"2057", // !=
								"2110", // !=
								"2058", // ==
								"2017", // ==
								"2169", // != Milton GO Station =>
								"2203" // != Milton GO Station =>
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								"2169", "2203", // Milton GO Station
								"2124", // ==
								"2407", // Serafini
								"2418" // == Etheridge
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(99_000L, new RouteTripSpec(99_000L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.NORTH.getId(), // Commercial
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_DIRECTION, MDirectionType.SOUTH.getId()) // Milton Sports Centre - Park & Ride
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
								"3017", // Milton Sports Centre - Park & Ride <= CONTINUE
								"2264", // Milton Sports Centre
								"2177", // Farmstead
								"2405" // Commercial
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
								"2405", // Commercial
								"2407", // Serafini
								"3017" // Milton Sports Centre - Park & Ride => CONTINUE
						)) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@NotNull
	@Override
	public String cleanStopOriginalId(@NotNull String gStopId) {
		gStopId = STARTS_WITH_MI.matcher(gStopId).replaceAll(StringUtils.EMPTY);
		gStopId = ENDS_WITH_T.matcher(gStopId).replaceAll(StringUtils.EMPTY);
		return gStopId;
	}

	@Override
	public int compareEarly(long routeId, @NotNull List<MTripStop> list1, @NotNull List<MTripStop> list2, @NotNull MTripStop ts1, @NotNull MTripStop ts2, @NotNull GStop ts1GStop, @NotNull GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@NotNull
	@Override
	public ArrayList<MTrip> splitTrip(@NotNull MRoute mRoute, @Nullable GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@NotNull
	@Override
	public Pair<Long[], Integer[]> splitTripStop(@NotNull MRoute mRoute, @NotNull GTrip gTrip, @NotNull GTripStop gTripStop, @NotNull ArrayList<MTrip> splitTrips, @NotNull GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		final String tripHeadsign = gTrip.getTripHeadsignOrDefault();
		if (mRoute.getId() == 1L + ROUTE_ID_ENDS_WITH_A) { // 1A
			if (gTrip.getDirectionId() == null) {
				if ("REGIONAL RD 25 & BRITANNIA".equals(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), 0);
					return;
				} else if ("MILTON GO".equals(tripHeadsign)) {
					mTrip.setHeadsignString(cleanTripHeadsign(tripHeadsign), 1);
					return;
				}
				throw new MTLog.Fatal("%s: Unexpected trips headsign for %s!", mTrip.getRouteId(), gTrip);
			}
		}
		mTrip.setHeadsignString(cleanTripHeadsign( //
				tripHeadsign), //
				gTrip.getDirectionId() == null ? 0 : gTrip.getDirectionId());
	}

	private static final Pattern STARTS_WITH_TO = Pattern.compile("(^to )", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		if (Utils.isUppercaseOnly(tripHeadsign, true, true)) {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		}
		tripHeadsign = STARTS_WITH_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 1L) {
			if (Arrays.asList( //
					StringUtils.EMPTY, //
					"Milton Fairgrounds", //
					"Milton Go" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Milton Go", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 2L) {
			if (Arrays.asList( //
					StringUtils.EMPTY, //
					"Crossroads Ctr", //
					"Milton Fairgrounds", //
					"Milton Go" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Milton Go", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 4L) {
			if (Arrays.asList( //
					"Louis St. Laurent & 4th", //
					"Regional Rd 25 & Britannia" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Louis St. Laurent & 4th", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 9L) {
			if (Arrays.asList( //
					"High Pt", //
					"Regional Rd 25 & Britannia" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Regional Rd 25 & Britannia", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 1L + ROUTE_ID_ENDS_WITH_A) { // 1A
			if (Arrays.asList( //
					"R.R. 25 & No. 5 Side Rd", //
					MILTON_GO).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(MILTON_GO, mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 50L) {
			if (Arrays.asList( //
					"Milton District", // <>
					"Scott & Derry", //
					"St Francis Xavier" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("St Francis Xavier", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 51L) {
			if (Arrays.asList( //
					"Milton District", // <>
					"Derry & Scott", //
					"St Francis Xavier" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("St Francis Xavier", mTrip.getHeadsignId());
				return true;
			}
		}
		if (mTrip.getRouteId() == 52L) {
			if (Arrays.asList( //
					"Milton Go", // <>
					"St Francis Xavier" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("St Francis Xavier", mTrip.getHeadsignId());
				return true;
			}
		}
		throw new MTLog.Fatal("%s: Unexpected trips to merge: %s & %s!", mTrip.getRouteId(), mTrip, mTripToMerge);
	}

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.cleanSlashes(gStopName);
		gStopName = CleanUtils.removePoints(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	private static final Pattern STARTS_WITH_MI = Pattern.compile("((^)(mi))", Pattern.CASE_INSENSITIVE);
	private static final Pattern ENDS_WITH_T = Pattern.compile("(t$)", Pattern.CASE_INSENSITIVE);

	@Override
	public int getStopId(@NotNull GStop gStop) {
		//noinspection deprecation
		String stopId = cleanStopOriginalId(gStop.getStopId());
		return Integer.parseInt(stopId);
	}
}
