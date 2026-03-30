package eu.europeana.api.dataset.generation.model;

import dev.morphia.annotations.*;
import eu.europeana.api.dataset.generation.utils.ModelConstants;
import org.bson.types.ObjectId;

import java.util.Date;

@Entity("DatasetReport")
@Indexes({
        @Index(
                fields = {@Field(ModelConstants.lastHarvestDate)})})
public class DatasetReport {

    @Id
    private ObjectId dbId;

    private Date lastHarvestDate;

    public Date getLastHarvestDate() {
        return lastHarvestDate;
    }

    public void setLastHarvestDate(Date lastHarvestDate) {
        this.lastHarvestDate = lastHarvestDate;
    }
}
