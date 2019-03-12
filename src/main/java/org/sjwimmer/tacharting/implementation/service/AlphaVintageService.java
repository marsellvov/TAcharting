package org.sjwimmer.tacharting.implementation.service;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import okhttp3.OkHttpClient;
import org.sjwimmer.tacharting.chart.model.TaTimeSeries;
import org.sjwimmer.tacharting.chart.model.types.TimeFormatType;
import org.sjwimmer.tacharting.chart.model.types.YahooTimePeriod;
import org.sjwimmer.tacharting.chart.parameters.Parameter;
import org.sjwimmer.tacharting.implementation.constants.Function;
import org.sjwimmer.tacharting.implementation.constants.Interval;
import org.sjwimmer.tacharting.implementation.constants.OutputSize;
import org.sjwimmer.tacharting.implementation.model.api.CSVConnector;
import org.sjwimmer.tacharting.implementation.model.api.YahooSettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AlphaVintageService extends Service<List<TaTimeSeries>> {
    private final Logger log = LoggerFactory.getLogger(YahooService.class);

    private static final String pattern = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter Parser = DateTimeFormatter.ofPattern(pattern);
    private static final ZoneId timeZone = ZoneId.systemDefault();
    private static final String REQ_BASE_URL = "https://www.alphavantage.co/query";
    private final Properties properties;
    private final String[] resources;


    public AlphaVintageService(String... resources){
        this.properties = YahooSettingsManager.getProperties();
        this.resources = resources;
    }

    @Override
    protected Task<List<TaTimeSeries>> createTask() {
        return new RequestTimeSeries();
    }


    class RequestTimeSeries extends Task<List<TaTimeSeries>> {

        @Override
        protected List<TaTimeSeries> call() throws Exception {
            List<TaTimeSeries> seriesList = new ArrayList<>();
            CSVConnector csvConnector = new CSVConnector();
            for(int i = 0; i < resources.length; i++) {
                Map<String, String> params = new LinkedHashMap<String, String>();
                params.put("function", Function.TIME_SERIES_INTRADAY.name());
                params.put("symbol", String.valueOf(resources[i]));
                params.put("interval", Interval.FIVE_MINUTE.getInterval());
                params.put("outputsize", OutputSize.FULL.getOutputSize());
                params.put("datatype", "csv");
                params.put("apikey", "4SP0KB6VSHEA908Q");

                String symbol = resources[i];
                updateProgress(i, resources.length - 1);
                updateMessage("Request data for " + symbol);
                String url = REQ_BASE_URL + URLEncoder.encode(symbol, "UTF-8") + "?" + createURLParameters(params);
                URL request = new URL(url);
                HttpURLConnection connection = null;
                int redirects = 0;
                boolean hasResponse = false;
                URL currentRequest = request;
                while (!hasResponse && redirects < 5) {
                    connection = (HttpURLConnection) currentRequest.openConnection();
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    connection.setInstanceFollowRedirects(true);

                    switch (connection.getResponseCode()) {
                        case HttpURLConnection.HTTP_MOVED_PERM:
                        case HttpURLConnection.HTTP_MOVED_TEMP:
                            redirects++;
                            String location = connection.getHeaderField("Location");
                            currentRequest = new URL(request, location);
                            break;
                        default:
                            hasResponse = true;
                    }
                }

                if (redirects > 5) {
                    throw new IOException("Protocol redirect count exceeded for url: " + request.toExternalForm());
                } else if (connection == null) {
                    throw new IOException("Unexpected error while opening connection");
                } else {
                    InputStreamReader is = new InputStreamReader(connection.getInputStream());
                    BufferedReader br = new BufferedReader(is);
                    File file = new File(Parameter.PROGRAM_FOLDER + Parameter.S + symbol+"temp.csv");
                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fos));
                    String header = br.readLine();
                    bufferedWriter.write(header);
                    for (String line = br.readLine(); line != null; line = br.readLine()) {
                        bufferedWriter.newLine();
                        bufferedWriter.write(line);
                    }
                    bufferedWriter.close();

                    seriesList.add(csvConnector.getSeriesFromAlphaVintageFile(symbol, file));
                    log.debug("{}",file.delete());
                }
            }

            return null;
        }

        private String createURLParameters(Map<String, String> params) {
            StringBuilder sb = new StringBuilder();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                String key = entry.getKey();
                String value = entry.getValue();
                try {
                    key = URLEncoder.encode(key, "UTF-8");
                    value = URLEncoder.encode(value, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    log.debug(ex.getMessage());
                }
                sb.append(String.format("%s=%s", key, value));
            }
            return sb.toString();
        }
    }
}
