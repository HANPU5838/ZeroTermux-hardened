package org.apache.ftpserver.listener.nio;



import com.termux.zerocore.ftp.new_ftp.services.FtpService;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import java.nio.charset.Charset;

public class FtpResponseEncoder extends ProtocolEncoderAdapter {
    /** Immutable, thread-safe charset; a fresh {@link java.nio.charset.CharsetEncoder} is created per encode. */
    private final Charset CHARSET;

    public FtpResponseEncoder() {
        CHARSET = Charset.forName(FtpService.getCharsetFromSharedPreferences());
        //Log.d(getClass().getName(),"the charset is "+FtpService.getCharsetFromSharedPreferences());
    }

    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
        String value = message.toString();
        IoBuffer buf = IoBuffer.allocate(value.length()).setAutoExpand(true);
        // A single ProtocolCodecFactory/FtpResponseEncoder instance is shared by all MINA sessions,
        // and CharsetEncoder is stateful and not thread-safe. Create one per call instead of
        // reusing a shared field, otherwise concurrent FTP sessions corrupt each other's output.
        buf.putString(value, CHARSET.newEncoder());
        buf.flip();
        out.write(buf);
    }
}
