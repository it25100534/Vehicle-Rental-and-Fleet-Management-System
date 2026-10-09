package com.example.vehiclerentalserviceplatform.fleet;

import com.example.vehiclerentalserviceplatform.security.InputValidation;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.vehiclerentalserviceplatform.persistence.VehicleCategoryEntity;
import com.example.vehiclerentalserviceplatform.persistence.VehicleCategoryRepository;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
public class VehicleCategoryService {
    private final File file;
    private final VehicleCategoryRepository repository;
    @Autowired
    public VehicleCategoryService(VehicleCategoryRepository repository) { this.repository=repository; this.file=new File("vehicle-categories.txt"); seedDatabase(); }

    public synchronized List<VehicleCategory> getAll() {
        return repository.findAll().stream().map(VehicleCategoryEntity::toDomain).toList();
    }
    public List<VehicleCategory> getActive(){return getAll().stream().filter(VehicleCategory::isActive).toList();}
    public boolean isActive(String code){return getAll().stream().anyMatch(c->c.getCode().equalsIgnoreCase(code)&&c.isActive());}
    public synchronized void save(String code,String name){
        String cleanCode=code==null?"":code.trim().toUpperCase(); InputValidation.requireVehicleAssignment(cleanCode,"BR-001");
        InputValidation.requireSafeText(name,"Category name",2,50);
        List<VehicleCategory> categories=new ArrayList<>(getAll());
        VehicleCategory existing=categories.stream().filter(c->c.getCode().equalsIgnoreCase(cleanCode)).findFirst().orElse(null);
        if(existing==null)categories.add(new VehicleCategory(cleanCode,name.trim(),true)); else {existing.setName(name.trim());existing.setActive(true);}
        VehicleCategoryEntity row=repository.findById(cleanCode).orElse(new VehicleCategoryEntity(cleanCode,name.trim(),true));row.update(name.trim(),true);repository.save(row);
    }
    public synchronized boolean setActive(String code,boolean active){List<VehicleCategory> categories=new ArrayList<>(getAll());VehicleCategory c=categories.stream().filter(x->x.getCode().equalsIgnoreCase(code)).findFirst().orElse(null);if(c==null)return false;c.setActive(active);VehicleCategoryEntity row=repository.findById(code.toUpperCase()).orElse(null);if(row==null)return false;row.setActive(active);repository.save(row);return true;}
    private void seedDatabase(){if(repository.count()>0)return;if(!file.exists()){for(String[]p:new String[][]{{"DAILY","Daily Travel"},{"FAMILY","Family Trips"},{"BUSINESS","Business Travel"},{"WEDDING","Weddings"},{"ADVENTURE","Adventure"},{"TRANSPORT","Transport"}})repository.save(new VehicleCategoryEntity(p[0],p[1],true));return;}try{for(String line:Files.readAllLines(file.toPath(),StandardCharsets.UTF_8)){String[] p=line.split(",",-1);if(p.length==3)repository.save(new VehicleCategoryEntity(p[0],p[1],Boolean.parseBoolean(p[2])));}}catch(IOException ex){throw new IllegalStateException("Could not import vehicle categories.",ex);}}
}
