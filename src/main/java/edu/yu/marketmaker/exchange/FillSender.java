package edu.yu.marketmaker.exchange;

import edu.yu.marketmaker.model.Fill;

public interface FillSender {
    
    public void sendFill(Fill fill);
}
