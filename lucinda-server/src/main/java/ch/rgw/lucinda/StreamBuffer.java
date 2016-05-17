package ch.rgw.lucinda;

/**
 * Created by gerry on 03.07.15.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class StreamBuffer extends Thread
{
    InputStream is;
    StringBuilder res;

    StreamBuffer(InputStream is, StringBuilder output)
    {
        this.is = is;
        this.res = output;
    }

    public void run()
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null)
                res.append(line).append("\n");
            } catch (IOException ioe)
              {
                ioe.printStackTrace();
              }
    }
}