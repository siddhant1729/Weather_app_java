import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import org.json.JSONObject;
import org.json.JSONArray;

class WeatherInfo {
    String city;
    String country;
    double temperature;
    double feelsLike;
    int humidity;
    int pressure;
    String condition;
    double windSpeed;
    int cloudiness;
    double visibilityKm;
    String sunrise;
    String sunset;
}

class HistoryManager {
    private static final String HISTORY_FILE = "weather_history.txt";

    public void save(String city) {
        try (FileWriter fw = new FileWriter(HISTORY_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            out.println(city + " at " + now.format(formatter));
        } catch (IOException e) {
            System.err.println("Error saving history: " + e.getMessage());
        }
    }

    public void show() {
        System.out.println("\n--- Search History ---");
        try (BufferedReader br = new BufferedReader(new FileReader(HISTORY_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (FileNotFoundException e) {
            System.out.println("No history found.");
        } catch (IOException e) {
            System.err.println("Error reading history: " + e.getMessage());
        }
    }
}

class ConsoleWeatherDisplay {
    public void typewriter(String text, int delayMs) {
        for (char c : text.toCharArray()) {
            System.out.print(c);
            System.out.flush();
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println();
    }

    public void typewriter(String text) {
        typewriter(text, 30);
    }

    public void show(WeatherInfo info) {
        typewriter("City: " + info.city + ", " + info.country);
        typewriter(String.format("Temperature: %.2f °C", info.temperature));
        typewriter(String.format("Feels Like: %.2f °C", info.feelsLike));
        typewriter("Condition: " + info.condition);
        typewriter("Humidity: " + info.humidity + "%");
        typewriter(String.format("Wind Speed: %.2f m/s", info.windSpeed));
        typewriter("Cloudiness: " + info.cloudiness + "%");
        typewriter(String.format("Visibility: %.2f km", info.visibilityKm));
        typewriter("Pressure: " + info.pressure + " hPa");
        typewriter("Sunrise: " + info.sunrise);
        typewriter("Sunset: " + info.sunset);
    }
}

class WeatherFetcher {
    private String apiKey;
    private HistoryManager history;
    private ConsoleWeatherDisplay display;

    public WeatherFetcher(String apiKey) {
        this.apiKey = apiKey;
        this.history = new HistoryManager();
        this.display = new ConsoleWeatherDisplay();
    }

    private String formatTime(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(timestamp), 
            ZoneId.systemDefault()
        );
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return dateTime.format(formatter);
    }

    private void loadingAnimation(String message, int duration) {
        char[] spinner = {'|', '/', '-', '\\'};
        System.out.print(message);
        try {
            for (int i = 0; i < duration * 10; i++) {
                System.out.print("\b" + spinner[i % 4]);
                System.out.flush();
                Thread.sleep(100);
            }
            System.out.print("\b \n");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean fetchWeather(String city, WeatherInfo info) {
        try {
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8.toString());
            String urlString = "https://api.openweathermap.org/data/2.5/weather?q=" 
                             + encodedCity + "&appid=" + apiKey + "&units=metric";
            
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            loadingAnimation("Fetching weather data...", 3);

            int responseCode = conn.getResponseCode();
            
            BufferedReader in;
            if (responseCode == 200) {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }

            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject data = new JSONObject(response.toString());

            if (data.has("cod") && data.getInt("cod") != 200) {
                System.err.println("API Error: " + data.getString("message"));
                return false;
            }

            info.city = data.getString("name");
            info.country = data.getJSONObject("sys").getString("country");
            info.temperature = data.getJSONObject("main").getDouble("temp");
            info.feelsLike = data.getJSONObject("main").getDouble("feels_like");
            info.humidity = data.getJSONObject("main").getInt("humidity");
            info.pressure = data.getJSONObject("main").getInt("pressure");
            info.condition = data.getJSONArray("weather").getJSONObject(0).getString("description");
            info.windSpeed = data.getJSONObject("wind").getDouble("speed");
            info.cloudiness = data.getJSONObject("clouds").getInt("all");
            info.visibilityKm = data.getDouble("visibility") / 1000.0;
            info.sunrise = formatTime(data.getJSONObject("sys").getLong("sunrise"));
            info.sunset = formatTime(data.getJSONObject("sys").getLong("sunset"));

            history.save(city);
            return true;

        } catch (Exception e) {
            System.err.println("Error fetching weather: " + e.getMessage());
            return false;
        }
    }

    public void displayWeather(WeatherInfo info) {
        display.show(info);
    }

    public void showHistory() {
        history.show();
    }

    public void showLocalTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        display.typewriter("Local Time: " + now.format(formatter));
    }
}

public class WeatherApp {
    public static void main(String[] args) {
        String apiKey = "3e5a9e976db7e1d1c268b3528bd1a7de"; // Replace with your actual API key
        WeatherFetcher weather = new WeatherFetcher(apiKey);
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n**************** Weather App Menu ****************");
            System.out.println("1. Fetch Weather");
            System.out.println("2. Show Search History");
            System.out.println("3. Show Local Time");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");
            
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            if (choice == 1) {
                System.out.print("Enter city name: ");
                String city = scanner.nextLine();
                WeatherInfo info = new WeatherInfo();
                if (weather.fetchWeather(city, info)) {
                    weather.displayWeather(info);
                }
            } else if (choice == 2) {
                weather.showHistory();
            } else if (choice == 3) {
                weather.showLocalTime();
            } else if (choice == 0) {
                System.out.println("Exiting weather fetcher");
                break;
            } else {
                System.out.println("Invalid choice. Try again.");
            }
        }

        scanner.close();
    }
}