flowchart TD
ExtOrderPub["External Order Pub"]
Exchange["Exchange"]
TradingState["Trading State"]
ExposureRes["Exposure Reservation"]
MarketMaker["Market Maker"]
PositionUI["Position UI"]

    ExtOrderPub -->|"Places buy and sell orders"| Exchange
    Exchange -->|"Sends fill data"| TradingState
    TradingState -->|"Sends position data"| MarketMaker
    TradingState -->|"Sends current position data"| PositionUI
    TradingState -->|"Updates exposures"| ExposureRes
    ExposureRes -->|"Warn if over exposed"| Exchange
    MarketMaker -->|"Updates quotes"| Exchange