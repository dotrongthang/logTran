package wo.external;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import com.opencsv.CSVReader;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class Main {

    private static String FILE_LOCATION = " ";

    private static String FILE_DB_LOCATION = " ";
    private static String FILE_OUTPUT = " ";

    private static String FILE_TYPE = " ";
    private static String API = "";
    private static String API_KEY = "";
    private static int IP_INDEX = -1;

    private static int COUNTRY_INDEX = -1;
    private static int TYPE = 0;

    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");


    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {

        logger.info("Convert Ip address to location: Start!");

        Properties properties = new Properties();
        InputStream inputStream = null;
        try {
            inputStream = Main.class.getClassLoader()
                    .getResourceAsStream("resource.properties");

            // load properties from file
            properties.load(inputStream);

            Calendar c = Calendar.getInstance();
            c.setTime(new Date()); // Using today's date
            c.add(Calendar.DATE, -1);
            String dateStr = simpleDateFormat.format(c.getTime());


            // get property by name
            if (FILE_LOCATION.equalsIgnoreCase(" ")) {
                FILE_LOCATION = properties.getProperty("file.location");
//                FILE_LOCATION = FILE_LOCATION + "LogTran_" + dateStr + ".xlsx";
                logger.info("Convert Ip address to location: Input file = " + FILE_LOCATION);
                FILE_OUTPUT = properties.getProperty("file.output");
//                FILE_OUTPUT = FILE_OUTPUT + "LogTran_" + dateStr + "_out.xlsx";
                logger.info("Convert Ip address to location: Output file = " + FILE_OUTPUT);
                FILE_TYPE = properties.getProperty("file.type");
                logger.info("Convert Ip address to location: File type = " + FILE_TYPE);
                FILE_DB_LOCATION = properties.getProperty("file.db");
                logger.info("Convert Ip address to location: File db = " + FILE_DB_LOCATION);
            }
            API = properties.getProperty("api");
            API_KEY = properties.getProperty("api.key");
            TYPE = Integer.parseInt(properties.getProperty("type"));

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // close objects
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {

            logger.info("Convert Ip address to location: Start reading file: " + FILE_LOCATION);

            if(FILE_TYPE.equalsIgnoreCase("CSV"))
                //read from csv file
            {
                Reader readerF = new FileReader(FILE_LOCATION);
                logger.info("Convert Ip address to location: Readed file: " + FILE_LOCATION);
                try (CSVReader reader = new CSVReader(readerF)) {
                    File csvOutputFile = new File(FILE_OUTPUT);
                    logger.info("Convert Ip address to location: Start writing to file: " + FILE_OUTPUT);

                    try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
                        String[] lineInArray;
                        while ((lineInArray = reader.readNext()) != null) {
//                            JSONObject json = getCountryFromIp(lineInArray[2]);
//                            pw.println(lineInArray[0] + "," + lineInArray[1] + "," + lineInArray[2] + "," + json.get("country_name") + "," + json.get("city"));
                        }
                    }
                }
            }else if (FILE_TYPE.equalsIgnoreCase("EXCEL")){

            // read from excel file ->>>
            FileInputStream file = new FileInputStream(new File(FILE_LOCATION));

//            Files.copy(new File(FILE_LOCATION).toPath(), new File("./tmp.xlsx").toPath(), StandardCopyOption.REPLACE_EXISTING);

            //create tmp file
//            FileInputStream file = new FileInputStream(new File("./tmp.xlsx"));


//            Create Workbook instance holding reference to .xlsx file
            XSSFWorkbook workbook2 = new XSSFWorkbook(file);

            //Get first/desired sheet from the workbook
            XSSFSheet sheet2 = workbook2.getSheetAt(0);

            //Iterate through each rows one by one
            Iterator<Row> rowIterator = sheet2.iterator();
            boolean firstRow = true;
            List<String> data = new ArrayList<>();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                //For each row, iterate through all the columns
                if (firstRow) {
                    //find position of IP column
                    Iterator<Cell> cellIterator = row.cellIterator();
                    int index = 0;
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        String name = cell.getStringCellValue();
                        if (name.equalsIgnoreCase("USER_IP")) {
                            IP_INDEX = index;
                        } else if (name.equalsIgnoreCase("COUNTRY_NM")) {
                            COUNTRY_INDEX = index;
                        }
                        index++;
                    }
                    firstRow = false;
                } else {
                    Iterator<Cell> cellIterator = row.cellIterator();
                    int index = 0;
                    while (cellIterator.hasNext()) {
                        Cell cell = cellIterator.next();
                        //if cell != IP column => continue
                        if (index != IP_INDEX) {
                            index++;
                            continue;
                        }
                        //Check the cell type and format accordingly
                        data.add(cell.getStringCellValue());
                        String country = getCountryFromIp(cell.getStringCellValue());

                        Cell cell3 = row.createCell(COUNTRY_INDEX);
                        cell3.setCellValue(country);
                        break;
                    }
                }
            }
            FileOutputStream out = new FileOutputStream(new File("./tmp.xlsx"));
            logger.info("Convert Ip address to location: Start writing to file: " + FILE_OUTPUT);
            FileOutputStream output = new FileOutputStream(new File(FILE_OUTPUT));
            workbook2.write(out);
            workbook2.write(output);
            file.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

//        System.out.println("Press enter to exit");
//        String end = new Scanner(System.in).nextLine();
        System.out.println("Convert Ip address to location: Finish!");
        logger.info("Convert Ip address to location: Finish!");
    }

    public static String getCountryFromIp(String ip) throws IOException, GeoIp2Exception {
        File database = new File(FILE_DB_LOCATION);
        try {
            DatabaseReader dbReader = new DatabaseReader.Builder(database).build();

            CountryResponse response = dbReader.country(InetAddress.getByName(ip));

//            logger.info("Convert Ip address to location: Ip: " + ip + " =>> country: " + response.getCountry().getName());
            return response.getCountry().getName();
        }catch (Exception e){
            return "";
        }
    }
}