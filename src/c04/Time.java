import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class Time {

  public static String passed(String from, String to) {
    try {
      boolean hasTime = from.contains("T") || to.contains("T");
      DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
      DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_DATE;

      ZonedDateTime start, end;

      if (hasTime) {
        start = LocalDateTime.parse(from, formatter).atZone(ZoneId.of("Europe/Warsaw"));
        end = LocalDateTime.parse(to, formatter).atZone(ZoneId.of("Europe/Warsaw"));
      } else {
        start = LocalDate.parse(from, dateFormatter).atStartOfDay(ZoneId.of("Europe/Warsaw"));
        end = LocalDate.parse(to, dateFormatter).atStartOfDay(ZoneId.of("Europe/Warsaw"));
      }

      long totalSeconds = ChronoUnit.SECONDS.between(start, end);
      double totalDays = totalSeconds / (24.0 * 60 * 60);
      long days = Math.round(totalDays);
      double weeks = totalDays / 7.0;

      long hours = hasTime ? ChronoUnit.HOURS.between(start, end) : 0;
      long minutes = hasTime ? ChronoUnit.MINUTES.between(start, end) : 0;

      Period period = Period.between(start.toLocalDate(), end.toLocalDate());
      int years = period.getYears();
      int months = period.getMonths();
      int remainingDays = period.getDays();

      StringBuilder result = new StringBuilder();

      result.append(String.format("Od %s do %s\n",
          getZonedTimeString(start),
          getZonedTimeString(end)));

      result.append(String.format(" - mija: %d " + getPolishDayLabel((int) days) + ", tygodni %.2f\n", days, weeks));

      if (hasTime) {
        result.append(String.format(" - godzin: %d, minut: %d\n", hours, minutes));
      }

      if (days > 0) {
        result.append(" - kalendarzowo: ");
        if (years > 0) {
          result.append(String.format("%d %s, ", years, getPolishYearLabel(years)));
        }
        if (months > 0) {
          result.append(String.format("%d %s, ", months, getPolishMonthLabel(months)));
        }
        if (remainingDays > 0) {
          result.append(String.format("%d %s", remainingDays, getPolishDayLabel(remainingDays)));
        }
        if (result.charAt(result.length() - 2) == ',')
          result.deleteCharAt(result.length() - 2);
        result.append("\n");
      }

      return result.toString().trim();

    } catch (DateTimeParseException e) {
      return "*** " + e.toString();
    }
  }

  private static String getZonedTimeString(ZonedDateTime zdt) {
    StringBuilder sb = new StringBuilder();
    sb.append(zdt.getDayOfMonth())
        .append(" ")
        .append(zdt.getMonth().getDisplayName(java.time.format.TextStyle.FULL, new Locale("pl")))
        .append(" ")
        .append(zdt.getYear())
        .append(" (")
        .append(zdt.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, new Locale("pl")))
        .append(")");
    if (!(zdt.getHour() == 0 && zdt.getMinute() == 0)) {
      sb.append(" godz. ")
          .append(zdt.getHour() + ":" + (zdt.getMinute() == 0 ? "00" : zdt.getMinute()));
    }
    return sb.toString();
  }

  private static String getPolishYearLabel(int years) {
    if (years == 1)
      return "rok";
    if (years == 12 || years == 13 || years == 14)
      return "lat";
    if ((years % 10) >= 2 && (years % 10) <= 4)
      return "lata";
    return "lat";
  }

  private static String getPolishMonthLabel(int months) {
    if (months == 1)
      return "miesiąc";
    if (months >= 2 && months <= 4)
      return "miesiące";
    return "miesięcy";
  }

  private static String getPolishDayLabel(int days) {
    if (days == 1)
      return "dzień";
    return "dni";
  }
}