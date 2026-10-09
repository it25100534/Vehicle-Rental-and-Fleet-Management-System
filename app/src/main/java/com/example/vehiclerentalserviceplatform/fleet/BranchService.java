package com.example.vehiclerentalserviceplatform.fleet;

import com.example.vehiclerentalserviceplatform.security.InputValidation;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.vehiclerentalserviceplatform.persistence.BranchEntity;
import com.example.vehiclerentalserviceplatform.persistence.BranchRepository;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
@org.springframework.context.annotation.DependsOn("featureSchemaMigration")
public class BranchService {
    private final File file;
    private final BranchRepository repository;
    @Autowired
    public BranchService(BranchRepository repository) { this.repository = repository; this.file = new File("branches.txt"); seedDatabase(); }

    public synchronized List<Branch> getAll() {
        return repository.findAll().stream().map(BranchEntity::toDomain).toList();
    }

    public List<Branch> getOpenBranches() { return getAll().stream().filter(Branch::isOpen).toList(); }
    public Branch find(String id) { return getAll().stream().filter(b -> b.getBranchId().equalsIgnoreCase(id)).findFirst().orElse(null); }
    public boolean isOpen(String id) { Branch b = find(id); return b != null && b.isOpen(); }

    public synchronized void save(String id, String name, String address, String phone) {
        String branchId = id == null ? "" : id.trim().toUpperCase();
        InputValidation.requireVehicleAssignment("DAILY", branchId);
        InputValidation.requireSafeText(name, "Branch name", 2, 60);
        InputValidation.requireSafeText(address, "Branch address", 5, 120);
        if (!InputValidation.isPhone(phone)) throw new IllegalArgumentException("Branch phone must contain exactly 10 digits and begin with 0.");
        List<Branch> branches = new ArrayList<>(getAll());
        Branch existing = branches.stream().filter(b -> b.getBranchId().equalsIgnoreCase(branchId)).findFirst().orElse(null);
        if (existing == null) branches.add(new Branch(branchId, name.trim(), address.trim(), phone.trim(), true));
        else { existing.setName(name.trim()); existing.setAddress(address.trim()); existing.setPhone(phone.trim()); }
        BranchEntity row = repository.findById(branchId).orElse(new BranchEntity(branchId, name.trim(), address.trim(), phone.trim(), true));
        row.update(name.trim(), address.trim(), phone.trim()); repository.save(row);
    }

    public synchronized boolean setOpen(String id, boolean open) {
        List<Branch> branches = new ArrayList<>(getAll());
        Branch branch = branches.stream().filter(b -> b.getBranchId().equalsIgnoreCase(id)).findFirst().orElse(null);
        if (branch == null) return false;
        branch.setOpen(open);
        BranchEntity row=repository.findById(id.toUpperCase()).orElse(null); if(row==null)return false; row.setOpen(open); repository.save(row); return true;
    }

    public synchronized void setImage(String id, String imagePath) {
        BranchEntity row = repository.findById(id.trim().toUpperCase()).orElseThrow(
                () -> new IllegalArgumentException("Branch was not found."));
        row.setImagePath(imagePath);
        repository.save(row);
    }

    public synchronized boolean delete(String id) {
        List<Branch> branches = new ArrayList<>(getAll());
        boolean removed = branches.removeIf(branch -> branch.getBranchId().equalsIgnoreCase(id));
        if (removed) repository.deleteById(id.toUpperCase());
        return removed;
    }

    private void seedDatabase() {
        if (repository.count() > 0) return;
        try {
            if (!file.exists()) {
                repository.save(new BranchEntity("BR-001","Colombo Central","01 Galle Road Colombo 03","0112345678",true));
                repository.save(new BranchEntity("BR-002","Kandy City","25 Peradeniya Road Kandy","0812345678",true));
                repository.save(new BranchEntity("BR-003","Galle Fort","10 Church Street Galle","0912345678",true));
                return;
            }
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                String[] p=line.split(",",-1);
                if(p.length==5) repository.save(new BranchEntity(p[0],p[1],p[2],p[3],Boolean.parseBoolean(p[4])));
            }
        } catch (IOException ex) { throw new IllegalStateException("Could not import branches.", ex); }
    }
}
