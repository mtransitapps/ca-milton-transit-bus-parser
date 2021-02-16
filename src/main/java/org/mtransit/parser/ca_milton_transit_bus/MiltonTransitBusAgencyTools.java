package org.mtransit.parser.ca_milton_transit_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.ColorUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mtransit.parser.StringUtils.EMPTY;

// https://milton.tmix.se/gtfs/gtfs-milton.zip
public class MiltonTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new MiltonTransitBusAgencyTools().start(args);
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "Milton Transit";
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if ("not in service".equalsIgnoreCase(gTrip.getTripHeadsign())) {
			return true;
		}
		return super.excludeTrip(gTrip);
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
		final String rsn = gRoute.getRouteShortName();
		if (!rsn.isEmpty() && CharUtils.isDigitsOnly(rsn)) {
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
		final Matcher matcher = DIGITS.matcher(rsn);
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

	private static final String ROUTE_COLOR_SCHOOL = "FFD800"; // School bus yellow

	@SuppressWarnings("DuplicateBranchesInSwitch")
	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
		String routeColor = gRoute.getRouteColor();
		if (ColorUtils.BLACK.equalsIgnoreCase(routeColor)) {
			routeColor = null;
		}
		if (StringUtils.isEmpty(routeColor)) {
			final String rsn = gRoute.getRouteShortName();
			if (CharUtils.isDigitsOnly(rsn)) {
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
		final String rlnS = gRoute.getRouteLongNameOrDefault();
		if (!StringUtils.isEmpty(rlnS)) {
			return cleanRouteLongName(rlnS);
		}
		final String rsnS = gRoute.getRouteShortName();
		if (CharUtils.isDigitsOnly(rsnS)) {
			final int rsn = Integer.parseInt(rsnS);
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

	private static final Pattern STARTS_WITH_RSN = Pattern.compile("(^[\\d]+[a-z]? (- )?)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String routeLongName) {
		routeLongName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, routeLongName, getIgnoredWords());
		routeLongName = STARTS_WITH_RSN.matcher(routeLongName).replaceAll(EMPTY);
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

	@NotNull
	@Override
	public String cleanStopOriginalId(@NotNull String gStopId) {
		gStopId = STARTS_WITH_MI.matcher(gStopId).replaceAll(EMPTY);
		gStopId = ENDS_WITH_T.matcher(gStopId).replaceAll(EMPTY);
		return gStopId;
	}

	@Override
	public boolean directionSplitterEnabled() {
		return true;
	}

	@Override
	public boolean directionSplitterEnabled(long routeId) {
		//noinspection RedundantIfStatement
		if (routeId == 2L) {
			return true;
		}
		return false;
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, tripHeadsign, getIgnoredWords());
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final String REMOVE_END_W_RLN_ = "( %s$)";

	@NotNull
	@Override
	public String cleanStopHeadSign(@NotNull GRoute gRoute, @NotNull GTrip gTrip, @NotNull GStopTime gStopTime, @NotNull String stopHeadsign) {
		if (!StringUtils.isEmpty(gRoute.getRouteLongName())) {
			stopHeadsign = stopHeadsign.replaceAll(String.format(REMOVE_END_W_RLN_, gRoute.getRouteLongNameOrDefault()), EMPTY);
		}
		return cleanStopHeadSign(stopHeadsign);
	}

	private String[] getIgnoredWords() {
		return new String[]{
				"NE", "SE", "NW", "SW",
				"GO",
		};
	}

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, gStopName, getIgnoredWords());
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = CleanUtils.cleanSlashes(gStopName);
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
