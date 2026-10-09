package com.example.vehiclerentalserviceplatform.customer.service;

import com.example.vehiclerentalserviceplatform.customer.model.RegularCustomer;
import com.example.vehiclerentalserviceplatform.model.User;
import com.example.vehiclerentalserviceplatform.persistence.CustomerEntity;
import com.example.vehiclerentalserviceplatform.persistence.CustomerRepository;
import com.example.vehiclerentalserviceplatform.security.PasswordHash;
import org.springframework.stereotype.Service;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Service
public class CustomerFileService {
    private static final File LEGACY_FILE=new File("customer.txt");
    private final CustomerRepository repository;
    public CustomerFileService(CustomerRepository repository){this.repository=repository;importLegacyCustomersWhenEmpty();}
    public synchronized boolean registerCustomer(User customer){if(!(customer instanceof RegularCustomer c))return false;String u=c.getUsername().trim();if(repository.existsByUsernameIgnoreCase(u)||repository.existsByLicenseIdIgnoreCase(c.getLicenseId().trim())||repository.existsByEmailIgnoreCase(c.getEmail().trim()))return false;repository.save(new CustomerEntity(u,c.getLicenseId().trim(),c.getEmail().trim(),c.getPhone().trim(),PasswordHash.encode(c.getPassword()),true));return true;}
    public boolean isLicenseIdTaken(String id){return id!=null&&repository.existsByLicenseIdIgnoreCase(id.trim());}
    public User findCustomer(String q){if(q==null)return null;return repository.findByUsernameIgnoreCase(q.trim()).or(()->repository.findByLicenseIdIgnoreCase(q.trim())).map(this::toCustomer).orElse(null);}
    public boolean isActiveCustomer(String u){return u!=null&&repository.findByUsernameIgnoreCase(u).map(CustomerEntity::isActive).orElse(false);}
    public boolean isDisabledCustomer(String u){return u!=null&&repository.findByUsernameIgnoreCase(u.trim()).map(e->!e.isActive()).orElse(false);}
    public synchronized boolean updateCustomer(String u,String email,String phone){CustomerEntity e=repository.findByUsernameIgnoreCase(u).orElse(null);if(e==null)return false;boolean duplicate=repository.findAll().stream().anyMatch(x->!x.getUsername().equalsIgnoreCase(u)&&x.getEmail().equalsIgnoreCase(email.trim()));if(duplicate)return false;e.setEmail(email.trim());e.setPhone(phone.trim());repository.save(e);return true;}
    public synchronized boolean changePassword(String u,String current,String next){CustomerEntity e=repository.findByUsernameIgnoreCase(u).orElse(null);if(e==null||!e.isActive()||!PasswordHash.matches(current,e.getPasswordHash()))return false;e.setPasswordHash(PasswordHash.encode(next));repository.save(e);return true;}
    public synchronized boolean resetPassword(String u,String email,String licence,String next){CustomerEntity e=repository.findByUsernameIgnoreCase(u.trim()).orElse(null);if(e==null||!e.isActive()||!e.getEmail().equalsIgnoreCase(email.trim())||!e.getLicenseId().equalsIgnoreCase(licence.trim()))return false;e.setPasswordHash(PasswordHash.encode(next));repository.save(e);return true;}
    public synchronized boolean deactivateCustomer(String u,String current){CustomerEntity e=repository.findByUsernameIgnoreCase(u).orElse(null);if(e==null||!e.isActive()||!PasswordHash.matches(current,e.getPasswordHash()))return false;e.setActive(false);repository.save(e);return true;}
    public synchronized boolean loginCustomer(String u,String password){if(u==null)return false;CustomerEntity e=repository.findByUsernameIgnoreCase(u.trim()).orElse(null);if(e==null||!e.isActive()||!PasswordHash.matches(password,e.getPasswordHash()))return false;if(!PasswordHash.isEncoded(e.getPasswordHash())){e.setPasswordHash(PasswordHash.encode(password));repository.save(e);}return true;}
    private void importLegacyCustomersWhenEmpty(){if(repository.count()!=0||!LEGACY_FILE.exists())return;try{for(String line:Files.readAllLines(LEGACY_FILE.toPath(), StandardCharsets.UTF_8)){String[]v=line.split(",",-1);if(v.length>=6&&"CUSTOMER".equals(v[0]))repository.save(new CustomerEntity(v[1],v[3],v[4],v[5],v[2],v.length<7||!"INACTIVE".equalsIgnoreCase(v[6])));}}catch(Exception ex){throw new IllegalStateException("Could not import customers.",ex);}}
    private RegularCustomer toCustomer(CustomerEntity e){return new RegularCustomer(e.getUsername(),e.getPasswordHash(),e.getLicenseId(),e.getEmail(),e.getPhone());}
}
