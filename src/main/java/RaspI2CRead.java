import java.util.HashMap;
import java.util.Map;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.platform.Platform;
import com.pi4j.platform.PlatformManager;

public class RaspI2CRead {
    private static final int DS3231_ADDR = 0x68;

    private static final int DS3231_TIME_SECONDS_ADDR = 0x00;
    private static final int DS3231_TIME_MINUTES_ADDR = 0x01;
    private static final int DS3231_TIME_HOURS_ADDR = 0x02;
    private static final int DS3231_TIME_WEEK_DAY_ADDR = 0x03;
    private static final int DS3231_TIME_DATE_ADDR = 0x04;
    private static final int DS3231_TIME_MONTH_CENTURY_ADDR = 0x05;
    private static final int DS3231_TIME_YEAR_ADDR = 0x06;
    private static final int DS3231_TEMPERATURE_ADDR_MSB = 0x11;
    private static final int DS3231_TEMPERATURE_ADDR_LSB = 0x12;

    public static Map<Integer, String> weekDay = new HashMap<Integer, String>() {
        private static final long serialVersionUID = 1193975786754897061L;
        {
            put(1, "Понедельник");
            put(2, "Вторник");
            put(3, "Среда");
            put(4, "Четверг");
            put(5, "Пятница");
            put(6, "Суббота");
            put(7, "Воскресенье");
        }
    };

    public static void main(String[] args) throws Exception {

        PlatformManager.setPlatform(Platform.RASPBERRYPI);

        I2CBus i2c = I2CFactory.getInstance(I2CBus.BUS_0);
        I2CDevice device = i2c.getDevice(DS3231_ADDR);

        int tempMsb = device.read(DS3231_TEMPERATURE_ADDR_MSB);
        int tempLsb = device.read(DS3231_TEMPERATURE_ADDR_LSB) >> 6;
        int nint;
        if ((tempMsb & 0x80) != 0) {
            nint = tempMsb | ~((1 << 8) - 1);
        } else {
            nint = tempMsb;
        }
        double temperature = 0.25 * tempLsb + nint;

        System.out.println("t = " + temperature + " °C");

        System.out.println(String.format("%s %s/%s/%s %s:%s:%s",
                weekDay.get(bcdToDec(device.read(DS3231_TIME_WEEK_DAY_ADDR))),
                bcdToDec(device.read(DS3231_TIME_DATE_ADDR)),
                bcdToDec(device.read(DS3231_TIME_MONTH_CENTURY_ADDR) & 0x1F),
                bcdToDec(device.read(DS3231_TIME_YEAR_ADDR)) + 2000,
                bcdToDec(device.read(DS3231_TIME_HOURS_ADDR)),
                bcdToDec(device.read(DS3231_TIME_MINUTES_ADDR)),
                bcdToDec(device.read(DS3231_TIME_SECONDS_ADDR))));

        Thread.sleep(1000);

        byte[] buffer = new byte[7];
        device.read(DS3231_TIME_SECONDS_ADDR, buffer, 0, buffer.length);
        System.out.println(String.format("%s %s/%s/%s %s:%s:%s",
                weekDay.get(bcdToDec(buffer[3])),
                bcdToDec(buffer[4]),
                bcdToDec(buffer[5] & 0x1F),
                bcdToDec(buffer[6]) + 2000,
                bcdToDec(buffer[2]),
                bcdToDec(buffer[1]),
                bcdToDec(buffer[0])));

    }

    static int bcdToDec(byte val) {
        int intVal = val & 0xFF;
        return ((intVal / 16 * 10) + (intVal % 16));
    }

    static int bcdToDec(int val) {
        return ((val / 16 * 10) + (val % 16));
    }
}