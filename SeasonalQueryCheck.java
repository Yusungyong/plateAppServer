import java.sql.*;

public class SeasonalQueryCheck {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://plateappdb.cp6806wks0x5.ap-northeast-2.rds.amazonaws.com:5432/mainproject_2";
        String user = "master_user";
        String password = "main1925";
        String today = "2026-05-05";

        Class.forName("org.postgresql.Driver");
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("[active fp_341]");
            try (PreparedStatement ps = conn.prepareStatement(
                    "select id, seasonal_term, start_date, end_date from fp_341 where start_date <= ? and end_date >= ? order by start_date asc, id asc")) {
                ps.setDate(1, java.sql.Date.valueOf(today));
                ps.setDate(2, java.sql.Date.valueOf(today));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        System.out.println(rs.getInt("id") + " | " + rs.getString("seasonal_term") + " | " + rs.getDate("start_date") + " | " + rs.getDate("end_date"));
                    }
                }
            }

            System.out.println("[fp_340 via active terms]");
            try (PreparedStatement ps = conn.prepareStatement(
                    "select f.id, f.month, f.seasonal_term, f.category, f.food_name from fp_340 f where f.seasonal_term in (select r.seasonal_term from fp_341 r where r.start_date <= ? and r.end_date >= ?) order by f.month asc, f.seasonal_term asc, f.category asc, f.food_name asc, f.id asc")) {
                ps.setDate(1, java.sql.Date.valueOf(today));
                ps.setDate(2, java.sql.Date.valueOf(today));
                try (ResultSet rs = ps.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        System.out.println(rs.getInt("id") + " | " + rs.getInt("month") + " | " + rs.getString("seasonal_term") + " | " + rs.getString("category") + " | " + rs.getString("food_name"));
                    }
                    System.out.println("count=" + count);
                }
            }

            System.out.println("[fp_340 month=5]");
            try (PreparedStatement ps = conn.prepareStatement(
                    "select id, month, seasonal_term, category, food_name from fp_340 where month = ? order by seasonal_term asc, category asc, food_name asc, id asc")) {
                ps.setInt(1, 5);
                try (ResultSet rs = ps.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        System.out.println(rs.getInt("id") + " | " + rs.getInt("month") + " | " + rs.getString("seasonal_term") + " | " + rs.getString("category") + " | " + rs.getString("food_name"));
                    }
                    System.out.println("count=" + count);
                }
            }
        }
    }
}