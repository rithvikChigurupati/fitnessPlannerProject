import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;

public class fitness {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/generate-plan", new WorkoutHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("Server started on port 8080");
    }

    static class WorkoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (t.getRequestMethod().equalsIgnoreCase("POST")) {
                // Add CORS headers to allow requests from localhost:8000
                t.getResponseHeaders().set("Access-Control-Allow-Origin", "http://localhost:8000");
                t.getResponseHeaders().set("Access-Control-Allow-Methods", "POST");
                t.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

                // Read the request body
                InputStreamReader isr = new InputStreamReader(t.getRequestBody(), "utf-8");
                BufferedReader br = new BufferedReader(isr);
                StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    requestBody.append(line);
                }

                // Parse the JSON request (simplified, assuming format: {"availableDays":["Monday","Wednesday"],"goal":"Bulk"})
                String body = requestBody.toString();
                List<String> days = new ArrayList<>();
                String goal = "Bulk"; // Default value

                // Simple parsing (you might want to use a JSON library like Jackson for robustness)
                if (body.contains("availableDays")) {
                    String daysStr = body.substring(body.indexOf("[") + 1, body.indexOf("]")).replace("\"", "");
                    if (!daysStr.isEmpty()) {
                        days = Arrays.asList(daysStr.split(","));
                    }
                }
                if (body.contains("goal")) {
                    goal = body.substring(body.indexOf("goal\":\"") + 7, body.indexOf("\"", body.indexOf("goal\":\"") + 8));
                }

                // Generate workout plan
                Map<String, String> plan = generatePlan(days, goal);

                // Convert plan to JSON
                String jsonResponse = "{\"dailyExercises\":" + mapToJson(plan) + "}";

                // Send response
                t.sendResponseHeaders(200, jsonResponse.length());
                OutputStream os = t.getResponseBody();
                os.write(jsonResponse.getBytes());
                os.close();
            } else {
                // Handle non-POST requests (e.g., OPTIONS for CORS preflight)
                if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                    t.getResponseHeaders().set("Access-Control-Allow-Origin", "http://localhost:8000");
                    t.getResponseHeaders().set("Access-Control-Allow-Methods", "POST");
                    t.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                    t.sendResponseHeaders(204, -1); // No content for OPTIONS
                    t.close();
                    return;
                }
                String response = "Method not supported";
                t.sendResponseHeaders(405, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }

        private Map<String, String> generatePlan(List<String> days, String goal) {
            Map<String, String> plan = new LinkedHashMap<>();
            Random random = new Random();
            List<String> muscleGroups = Arrays.asList("Chest", "Back", "Biceps", "Triceps", "Shoulders", "Legs");

            // Define pools of exercises for each muscle group and goal
            Map<String, Map<String, List<String>>> exercisesByGoal = new HashMap<>();

            // Bulk exercises (strength-focused, 6-12 reps, 3-5 sets)
            exercisesByGoal.put("Bulk", new HashMap<>());
            exercisesByGoal.get("Bulk").put("Chest", Arrays.asList(
                "Bench Press - %d sets of %d reps",
                "Incline Dumbbell Press - %d sets of %d reps",
                "Dumbbell Flys - %d sets of %d reps"
            ));
            exercisesByGoal.get("Bulk").put("Back", Arrays.asList(
                "Deadlift - %d sets of %d reps",
                "Pull-Ups - %d sets of %d reps",
                "Bent-Over Row - %d sets of %d reps"
            ));
            exercisesByGoal.get("Bulk").put("Biceps", Arrays.asList(
                "Barbell Curl - %d sets of %d reps",
                "Hammer Curl - %d sets of %d reps",
                "Preacher Curl - %d sets of %d reps"
            ));
            exercisesByGoal.get("Bulk").put("Triceps", Arrays.asList(
                "Close-Grip Bench Press - %d sets of %d reps",
                "Tricep Dips - %d sets of %d reps",
                "Skull Crushers - %d sets of %d reps"
            ));
            exercisesByGoal.get("Bulk").put("Shoulders", Arrays.asList(
                "Overhead Press - %d sets of %d reps",
                "Lateral Raises - %d sets of %d reps",
                "Front Raises - %d sets of %d reps"
            ));
            exercisesByGoal.get("Bulk").put("Legs", Arrays.asList(
                "Squat - %d sets of %d reps",
                "Leg Press - %d sets of %d reps",
                "Lunges - %d sets of %d reps"
            ));

            // Cut exercises (endurance-focused, 12-20 reps, 3-4 sets)
            exercisesByGoal.put("Cut", new HashMap<>());
            exercisesByGoal.get("Cut").put("Chest", Arrays.asList(
                "Bench Press - %d sets of %d reps",
                "Push-Ups - %d sets of %d reps",
                "Dumbbell Flys - %d sets of %d reps"
            ));
            exercisesByGoal.get("Cut").put("Back", Arrays.asList(
                "Bent-Over Row - %d sets of %d reps",
                "Lat Pulldown - %d sets of %d reps",
                "Seated Cable Row - %d sets of %d reps"
            ));
            exercisesByGoal.get("Cut").put("Biceps", Arrays.asList(
                "Dumbbell Curl - %d sets of %d reps",
                "Concentration Curl - %d sets of %d reps",
                "Cable Curl - %d sets of %d reps"
            ));
            exercisesByGoal.get("Cut").put("Triceps", Arrays.asList(
                "Tricep Pushdown - %d sets of %d reps",
                "Overhead Tricep Extension - %d sets of %d reps",
                "Tricep Kickbacks - %d sets of %d reps"
            ));
            exercisesByGoal.get("Cut").put("Shoulders", Arrays.asList(
                "Arnold Press - %d sets of %d reps",
                "Lateral Raises - %d sets of %d reps",
                "Front Raises - %d sets of %d reps"
            ));
            exercisesByGoal.get("Cut").put("Legs", Arrays.asList(
                "Lunges - %d sets of %d reps",
                "Leg Curl - %d sets of %d reps",
                "Calf Raises - %d sets of %d reps"
            ));

            // Randomize muscle groups for the available days (excluding rest days)
            List<String> workoutDays = new ArrayList<>(days);
            Collections.shuffle(workoutDays, random); // Randomize the order of days
            List<String> shuffledMuscleGroups = new ArrayList<>(muscleGroups);
            Collections.shuffle(shuffledMuscleGroups, random); // Randomize muscle groups

            int muscleIndex = 0;
            for (int i = 0; i < days.size(); i++) {
                String day = days.get(i);
                if (i > 0 && i % 3 == 0 && days.size() - i >= 2) { // Add rest day every 3rd day if possible
                    plan.put(day, "Rest Day"); // Ensure rest day text is consistently "Rest Day"
                } else {
                    // Use shuffled muscle groups and cycle through them
                    if (muscleIndex >= shuffledMuscleGroups.size()) {
                        Collections.shuffle(shuffledMuscleGroups, random); // Reset and reshuffle if we run out
                        muscleIndex = 0;
                    }
                    String muscle = shuffledMuscleGroups.get(muscleIndex);
                    Map<String, List<String>> exercises = exercisesByGoal.get(goal);
                    List<String> exerciseTemplates = exercises.get(muscle);

                    // Generate 2–3 randomized exercises for the day
                    int numExercises = random.nextInt(2) + 2; // Randomly choose 2 or 3 exercises
                    List<String> dailyExercises = new ArrayList<>();
                    Set<Integer> usedIndices = new HashSet<>(); // To avoid duplicate exercises

                    for (int j = 0; j < numExercises; j++) {
                        int index;
                        do {
                            index = random.nextInt(exerciseTemplates.size());
                        } while (usedIndices.contains(index)); // Ensure no duplicates
                        usedIndices.add(index);
                        String exerciseTemplate = exerciseTemplates.get(index);

                        // Randomize sets and reps based on goal
                        int sets, reps;
                        if (goal.equalsIgnoreCase("Bulk")) {
                            sets = random.nextInt(3) + 3; // 3-5 sets
                            reps = random.nextInt(7) + 6; // 6-12 reps
                        } else { // Cut
                            sets = random.nextInt(2) + 3; // 3-4 sets
                            reps = random.nextInt(9) + 12; // 12-20 reps
                        }

                        // Format the exercise with randomized sets and reps
                        String formattedExercise = String.format(exerciseTemplate, sets, reps);
                        dailyExercises.add(formattedExercise);
                    }

                    // Join multiple exercises with commas for the day
                    String dailyPlan = muscle + " Focus: " + String.join(", ", dailyExercises);
                    plan.put(day, dailyPlan);
                    muscleIndex++;
                }
            }

            return plan;
        }

        private String mapToJson(Map<String, String> map) {
            StringBuilder json = new StringBuilder("{");
            for (Map.Entry<String, String> entry : map.entrySet()) {
                json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue().replace("\"", "\\\"")).append("\",");
            }
            if (!map.isEmpty()) {
                json.setLength(json.length() - 1); // Remove trailing comma
            }
            json.append("}");
            return json.toString();
        }
    }
}