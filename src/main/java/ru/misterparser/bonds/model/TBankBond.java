package ru.misterparser.bonds.model;

import lombok.Data;

@Data
public class TBankBond {
    private String instrumentUid;
    private String figi;
    private String ticker;
    private String assetUid;
    private String brandName;
}