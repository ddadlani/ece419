import java.net.*;
import java.io.*;
import java.lang.*;

public class Stock {
        private String symbol;
        private Long quote;
        
        public Stock ()
        {
        }
        public Stock (String symbol, Long quote) {
                this.symbol = symbol;
                this.quote = quote;
        }

        public Long getQuote() {
                return this.quote;
        }
        
        public String getSymbol() {
                return this.symbol;
        }
        
        public void setSymbol(String symbol_) {
                this.symbol = symbol_;
        }
        
        public void setQuote(Long quote_) {
                this.quote = quote_;
        }
        
        //@Override
        //public boolean equals(Stock other) {
        //        return (other.symbol.equals(this.symbol));
        //        }        
}
        
