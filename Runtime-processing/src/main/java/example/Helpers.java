package example;

public final class Helpers {

    public static String readEnvVariable(String key) {
        String envVariable = System.getenv(key);
        if (envVariable == null) throw new NullPointerException("Error! Environment variable " + key + " is missing");
        return envVariable;
    }
}
