package com.nearnow.vendor;
import jakarta.validation.constraints.NotNull; import jakarta.validation.constraints.Positive; import jakarta.validation.constraints.Size;
public class RestockRequestDTO { @NotNull @Positive private Integer quantity; @Size(max=1000) private String note; public RestockRequestDTO(){} public Integer getQuantity(){return quantity;} public void setQuantity(Integer q){quantity=q;} public String getNote(){return note;} public void setNote(String n){note=n;} }
