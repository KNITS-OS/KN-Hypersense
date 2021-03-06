package com.knits.smartfactory.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.knits.smartfactory.IntegrationTest;
import com.knits.smartfactory.domain.ProductData;
import com.knits.smartfactory.repository.ProductDataRepository;
import com.knits.smartfactory.service.dto.ProductDataDTO;
import com.knits.smartfactory.service.mapper.ProductDataMapper;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link ProductDataResource} REST controller.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser
class ProductDataResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final Integer DEFAULT_SCRAPED_QTY = 1;
    private static final Integer UPDATED_SCRAPED_QTY = 2;

    private static final Integer DEFAULT_PENDING_QTY = 1;
    private static final Integer UPDATED_PENDING_QTY = 2;

    private static final Integer DEFAULT_REJECTED_QTY = 1;
    private static final Integer UPDATED_REJECTED_QTY = 2;

    private static final Integer DEFAULT_COMPLETED_QTY = 1;
    private static final Integer UPDATED_COMPLETED_QTY = 2;

    private static final String ENTITY_API_URL = "/api/product-data";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong count = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ProductDataRepository productDataRepository;

    @Autowired
    private ProductDataMapper productDataMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private MockMvc restProductDataMockMvc;

    private ProductData productData;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ProductData createEntity(EntityManager em) {
        ProductData productData = new ProductData()
            .name(DEFAULT_NAME)
            .scrapedQty(DEFAULT_SCRAPED_QTY)
            .pendingQty(DEFAULT_PENDING_QTY)
            .rejectedQty(DEFAULT_REJECTED_QTY)
            .completedQty(DEFAULT_COMPLETED_QTY);
        return productData;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static ProductData createUpdatedEntity(EntityManager em) {
        ProductData productData = new ProductData()
            .name(UPDATED_NAME)
            .scrapedQty(UPDATED_SCRAPED_QTY)
            .pendingQty(UPDATED_PENDING_QTY)
            .rejectedQty(UPDATED_REJECTED_QTY)
            .completedQty(UPDATED_COMPLETED_QTY);
        return productData;
    }

    @BeforeEach
    public void initTest() {
        productData = createEntity(em);
    }

    @Test
    @Transactional
    void createProductData() throws Exception {
        int databaseSizeBeforeCreate = productDataRepository.findAll().size();
        // Create the ProductData
        ProductDataDTO productDataDTO = productDataMapper.toDto(productData);
        restProductDataMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(productDataDTO))
            )
            .andExpect(status().isCreated());

        // Validate the ProductData in the database
        List<ProductData> productDataList = productDataRepository.findAll();
        assertThat(productDataList).hasSize(databaseSizeBeforeCreate + 1);
        ProductData testProductData = productDataList.get(productDataList.size() - 1);
        assertThat(testProductData.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testProductData.getScrapedQty()).isEqualTo(DEFAULT_SCRAPED_QTY);
        assertThat(testProductData.getPendingQty()).isEqualTo(DEFAULT_PENDING_QTY);
        assertThat(testProductData.getRejectedQty()).isEqualTo(DEFAULT_REJECTED_QTY);
        assertThat(testProductData.getCompletedQty()).isEqualTo(DEFAULT_COMPLETED_QTY);
    }

    @Test
    @Transactional
    void createProductDataWithExistingId() throws Exception {
        // Create the ProductData with an existing ID
        productData.setId(1L);
        ProductDataDTO productDataDTO = productDataMapper.toDto(productData);

        int databaseSizeBeforeCreate = productDataRepository.findAll().size();

        // An entity with an existing ID cannot be created, so this API call must fail
        restProductDataMockMvc
            .perform(
                post(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(productDataDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ProductData in the database
        List<ProductData> productDataList = productDataRepository.findAll();
        assertThat(productDataList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    void getAllProductData() throws Exception {
        // Initialize the database
        productDataRepository.saveAndFlush(productData);

        // Get all the productDataList
        restProductDataMockMvc
            .perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(productData.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME)))
            .andExpect(jsonPath("$.[*].scrapedQty").value(hasItem(DEFAULT_SCRAPED_QTY)))
            .andExpect(jsonPath("$.[*].pendingQty").value(hasItem(DEFAULT_PENDING_QTY)))
            .andExpect(jsonPath("$.[*].rejectedQty").value(hasItem(DEFAULT_REJECTED_QTY)))
            .andExpect(jsonPath("$.[*].completedQty").value(hasItem(DEFAULT_COMPLETED_QTY)));
    }

    @Test
    @Transactional
    void getProductData() throws Exception {
        // Initialize the database
        productDataRepository.saveAndFlush(productData);

        // Get the productData
        restProductDataMockMvc
            .perform(get(ENTITY_API_URL_ID, productData.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(productData.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME))
            .andExpect(jsonPath("$.scrapedQty").value(DEFAULT_SCRAPED_QTY))
            .andExpect(jsonPath("$.pendingQty").value(DEFAULT_PENDING_QTY))
            .andExpect(jsonPath("$.rejectedQty").value(DEFAULT_REJECTED_QTY))
            .andExpect(jsonPath("$.completedQty").value(DEFAULT_COMPLETED_QTY));
    }

    @Test
    @Transactional
    void getNonExistingProductData() throws Exception {
        // Get the productData
        restProductDataMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void putNewProductData() throws Exception {
        // Initialize the database
        productDataRepository.saveAndFlush(productData);

        int databaseSizeBeforeUpdate = productDataRepository.findAll().size();

        // Update the productData
        ProductData updatedProductData = productDataRepository.findById(productData.getId()).get();
        // Disconnect from session so that the updates on updatedProductData are not directly saved in db
        em.detach(updatedProductData);
        updatedProductData
            .name(UPDATED_NAME)
            .scrapedQty(UPDATED_SCRAPED_QTY)
            .pendingQty(UPDATED_PENDING_QTY)
            .rejectedQty(UPDATED_REJECTED_QTY)
            .completedQty(UPDATED_COMPLETED_QTY);
        ProductDataDTO productDataDTO = productDataMapper.toDto(updatedProductData);

        restProductDataMockMvc
            .perform(
                put(ENTITY_API_URL_ID, productDataDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(productDataDTO))
            )
            .andExpect(status().isOk());

        // Validate the ProductData in the database
        List<ProductData> productDataList = productDataRepository.findAll();
        assertThat(productDataList).hasSize(databaseSizeBeforeUpdate);
        ProductData testProductData = productDataList.get(productDataList.size() - 1);
        assertThat(testProductData.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testProductData.getScrapedQty()).isEqualTo(UPDATED_SCRAPED_QTY);
        assertThat(testProductData.getPendingQty()).isEqualTo(UPDATED_PENDING_QTY);
        assertThat(testProductData.getRejectedQty()).isEqualTo(UPDATED_REJECTED_QTY);
        assertThat(testProductData.getCompletedQty()).isEqualTo(UPDATED_COMPLETED_QTY);
    }

    @Test
    @Transactional
    void putNonExistingProductData() throws Exception {
        int databaseSizeBeforeUpdate = productDataRepository.findAll().size();
        productData.setId(count.incrementAndGet());

        // Create the ProductData
        ProductDataDTO productDataDTO = productDataMapper.toDto(productData);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restProductDataMockMvc
            .perform(
                put(ENTITY_API_URL_ID, productDataDTO.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(productDataDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ProductData in the database
        List<ProductData> productDataList = productDataRepository.findAll();
        assertThat(productDataList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithIdMismatchProductData() throws Exception {
        int databaseSizeBeforeUpdate = productDataRepository.findAll().size();
        productData.setId(count.incrementAndGet());

        // Create the ProductData
        ProductDataDTO productDataDTO = productDataMapper.toDto(productData);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProductDataMockMvc
            .perform(
                put(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtil.convertObjectToJsonBytes(productDataDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ProductData in the database
        List<ProductData> productDataList = productDataRepository.findAll();
        assertThat(productDataList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void putWithMissingIdPathParamProductData() throws Exception {
        int databaseSizeBeforeUpdate = productDataRepository.findAll().size();
        productData.setId(count.incrementAndGet());

        // Create the ProductData
        ProductDataDTO productDataDTO = productDataMapper.toDto(productData);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProductDataMockMvc
            .perform(put(ENTITY_API_URL).contentType(MediaType.APPLICATION_JSON).content(TestUtil.convertObjectToJsonBytes(productDataDTO)))
            .andExpect(status().isMethodNotAllowed());

        // Validate the ProductData in the database
        List<ProductData> productDataList = productDataRepository.findAll();
        assertThat(productDataList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void partialUpdateProductDataWithPatch() throws Exception {
        // Initialize the database
        productDataRepository.saveAndFlush(productData);

        int databaseSizeBeforeUpdate = productDataRepository.findAll().size();

        // Update the productData using partial update
        ProductData partialUpdatedProductData = new ProductData();
        partialUpdatedProductData.setId(productData.getId());

        partialUpdatedProductData.name(UPDATED_NAME).pendingQty(UPDATED_PENDING_QTY);

        restProductDataMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedProductData.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedProductData))
            )
            .andExpect(status().isOk());

        // Validate the ProductData in the database
        List<ProductData> productDataList = productDataRepository.findAll();
        assertThat(productDataList).hasSize(databaseSizeBeforeUpdate);
        ProductData testProductData = productDataList.get(productDataList.size() - 1);
        assertThat(testProductData.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testProductData.getScrapedQty()).isEqualTo(DEFAULT_SCRAPED_QTY);
        assertThat(testProductData.getPendingQty()).isEqualTo(UPDATED_PENDING_QTY);
        assertThat(testProductData.getRejectedQty()).isEqualTo(DEFAULT_REJECTED_QTY);
        assertThat(testProductData.getCompletedQty()).isEqualTo(DEFAULT_COMPLETED_QTY);
    }

    @Test
    @Transactional
    void fullUpdateProductDataWithPatch() throws Exception {
        // Initialize the database
        productDataRepository.saveAndFlush(productData);

        int databaseSizeBeforeUpdate = productDataRepository.findAll().size();

        // Update the productData using partial update
        ProductData partialUpdatedProductData = new ProductData();
        partialUpdatedProductData.setId(productData.getId());

        partialUpdatedProductData
            .name(UPDATED_NAME)
            .scrapedQty(UPDATED_SCRAPED_QTY)
            .pendingQty(UPDATED_PENDING_QTY)
            .rejectedQty(UPDATED_REJECTED_QTY)
            .completedQty(UPDATED_COMPLETED_QTY);

        restProductDataMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, partialUpdatedProductData.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(partialUpdatedProductData))
            )
            .andExpect(status().isOk());

        // Validate the ProductData in the database
        List<ProductData> productDataList = productDataRepository.findAll();
        assertThat(productDataList).hasSize(databaseSizeBeforeUpdate);
        ProductData testProductData = productDataList.get(productDataList.size() - 1);
        assertThat(testProductData.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testProductData.getScrapedQty()).isEqualTo(UPDATED_SCRAPED_QTY);
        assertThat(testProductData.getPendingQty()).isEqualTo(UPDATED_PENDING_QTY);
        assertThat(testProductData.getRejectedQty()).isEqualTo(UPDATED_REJECTED_QTY);
        assertThat(testProductData.getCompletedQty()).isEqualTo(UPDATED_COMPLETED_QTY);
    }

    @Test
    @Transactional
    void patchNonExistingProductData() throws Exception {
        int databaseSizeBeforeUpdate = productDataRepository.findAll().size();
        productData.setId(count.incrementAndGet());

        // Create the ProductData
        ProductDataDTO productDataDTO = productDataMapper.toDto(productData);

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restProductDataMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, productDataDTO.getId())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(productDataDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ProductData in the database
        List<ProductData> productDataList = productDataRepository.findAll();
        assertThat(productDataList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithIdMismatchProductData() throws Exception {
        int databaseSizeBeforeUpdate = productDataRepository.findAll().size();
        productData.setId(count.incrementAndGet());

        // Create the ProductData
        ProductDataDTO productDataDTO = productDataMapper.toDto(productData);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProductDataMockMvc
            .perform(
                patch(ENTITY_API_URL_ID, count.incrementAndGet())
                    .contentType("application/merge-patch+json")
                    .content(TestUtil.convertObjectToJsonBytes(productDataDTO))
            )
            .andExpect(status().isBadRequest());

        // Validate the ProductData in the database
        List<ProductData> productDataList = productDataRepository.findAll();
        assertThat(productDataList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void patchWithMissingIdPathParamProductData() throws Exception {
        int databaseSizeBeforeUpdate = productDataRepository.findAll().size();
        productData.setId(count.incrementAndGet());

        // Create the ProductData
        ProductDataDTO productDataDTO = productDataMapper.toDto(productData);

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        restProductDataMockMvc
            .perform(
                patch(ENTITY_API_URL).contentType("application/merge-patch+json").content(TestUtil.convertObjectToJsonBytes(productDataDTO))
            )
            .andExpect(status().isMethodNotAllowed());

        // Validate the ProductData in the database
        List<ProductData> productDataList = productDataRepository.findAll();
        assertThat(productDataList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    void deleteProductData() throws Exception {
        // Initialize the database
        productDataRepository.saveAndFlush(productData);

        int databaseSizeBeforeDelete = productDataRepository.findAll().size();

        // Delete the productData
        restProductDataMockMvc
            .perform(delete(ENTITY_API_URL_ID, productData.getId()).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        // Validate the database contains one less item
        List<ProductData> productDataList = productDataRepository.findAll();
        assertThat(productDataList).hasSize(databaseSizeBeforeDelete - 1);
    }
}
